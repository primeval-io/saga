package io.primeval.saga.core.test.rules;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.osgi.util.promise.Promise;

import com.google.common.collect.ImmutableList;

import io.primeval.codex.publisher.UnicastPublisher;
import io.primeval.codex.scheduler.Scheduler;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.action.Result;
import io.primeval.saga.annotations.Body;
import io.primeval.saga.annotations.QueryParameter;
import io.primeval.saga.annotations.Route;
import io.primeval.saga.controller.Controller;
import io.primeval.saga.http.protocol.HttpMethod;
import io.primeval.saga.http.protocol.HttpRequest;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.renderer.MimeTypes;
import io.primeval.saga.websocket.WebSocket;
import io.primeval.saga.websocket.WebSocketManager;
import io.primeval.saga.websocket.message.WebSocketMessage;
import io.primeval.saga.websocket.message.WebSocketMessage.Visitor;
import reactor.core.publisher.Flux;

@Controller
public final class TestController {

    private WebSocketManager webSocketManager;
    private Scheduler scheduler;

    @Route(method = HttpMethod.GET, uri = "simpleGet")
    public Result<String> simpleGet() {
        return Result.ok("Hello World").contentType(MimeTypes.JSON).withHeader("X-Test", "Foobar");
    }

    @Route(method = HttpMethod.POST, uri = "uppercase")
    public String uppercase(@Body String s) {
        return s.toUpperCase();
    }

    @Route(method = HttpMethod.GET, uri = "plainText")
    public String plainText() {
        return "HELLO!";
    }

    @Route(method = HttpMethod.GET, uri = "hello")
    public Result<String> hello(@QueryParameter String who) {
        return Result.ok("Hello " + who).contentType(MimeTypes.TEXT);
    }

    @Route(method = HttpMethod.GET, uri = "helloOptional")
    public Result<String> hello(@QueryParameter Optional<String> who) {
        return Result.ok("Hello " + who.orElse("unknown person")).contentType(MimeTypes.TEXT);
    }

    @Route(method = HttpMethod.POST, uri = "intOptional")
    public Result<Integer> inttt(@Body Integer i) {
        return Result.ok(i * 2).contentType(MimeTypes.JSON);
    }

    @Route(method = HttpMethod.GET, uri = "ws")
    public Promise<Result<Payload>> ws(HttpRequest request, @Body Payload payload) {
        UnicastPublisher<WebSocketMessage<Object>> publisher = webSocketManager.publisher();
        WebSocket<Object> webSocket = webSocketManager.createWebSocket(request, payload, publisher,
                TypeTag.of(Object.class));

        Flux.from(webSocket.incoming()).doOnNext(message -> message.visit(new Visitor<Object>() {

            @Override
            public void visitObject(Object object) {
                System.out.println(object);
            }

            @Override
            public void visitText(String text) {
                System.out.println(text);

            }
        })).subscribe();

        publisher.next(WebSocketMessage.text("Hello world"));

        publisher.next(WebSocketMessage.object(ImmutableList.of("Hello", "world")));

        scheduler.schedule(() -> {
            System.out.println("closing WS");
            publisher.complete();
        }, 9, TimeUnit.SECONDS);

        return webSocket.result();

    }

    public void setWebSocketManager(WebSocketManager webSocketManager) {
        this.webSocketManager = webSocketManager;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
}
