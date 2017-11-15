package io.primeval.saga.examples.helloworld;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;

import com.google.common.collect.ImmutableList;

import io.primeval.codex.io.resource.ReactiveResourceReader;
import io.primeval.codex.publisher.UnicastPublisher;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.action.Result;
import io.primeval.saga.annotations.Body;
import io.primeval.saga.annotations.PathParameter;
import io.primeval.saga.annotations.QueryParameter;
import io.primeval.saga.annotations.Route;
import io.primeval.saga.annotations.ext.ContentType;
import io.primeval.saga.controller.Controller;
import io.primeval.saga.guava.ImmutableResult;
import io.primeval.saga.http.protocol.HttpMethod;
import io.primeval.saga.http.protocol.HttpRequest;
import io.primeval.saga.http.protocol.Status;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.renderer.MimeTypes;
import io.primeval.saga.websocket.WebSocket;
import io.primeval.saga.websocket.WebSocketManager;
import io.primeval.saga.websocket.message.WebSocketMessage;
import reactor.core.publisher.Flux;

@Component
@Controller
public final class HelloWorldController {

    @Reference
    private WebSocketManager webSocketManager;

    @Reference
    public ReactiveResourceReader reactiveResourceReader;
    
    
    @Activate
    public void activate() {
        System.out.println("Super hello");
    }

    @Route(method = HttpMethod.GET, uri = "hello")
    public String hello(@QueryParameter String who) {
        return "Hello " + who;
    }
    
    @Route(method = HttpMethod.GET, uri = "hello2")
    public String hello2(@QueryParameter String who) {
        return "Hello " + who + "!!";
    }

    @Route(method = HttpMethod.GET, uri = "ingredients")
    public ImmutableList<String> ingredients() {
        return ImmutableList.of("Eggs", "Flour", "Milk");
    }

    @Route(method = HttpMethod.GET, uri = "item")
    public String item(@QueryParameter Integer id) {
        if (id == 42) {
            return "Foo";
        }
        throw new NoSuchElementException("Unknown item " + id);
    }

    @Route(method = HttpMethod.GET, uri = "emptyResult")
    public Result<List<String>> emptyResult() {
        return Result.create(Status.GONE);
    }

    @Route(method = HttpMethod.GET, uri = "emptyFluent")
    public Void emptyFluent() {
        return null;
    }

    @Route(method = HttpMethod.GET, uri = "location/{uuid}/foo")
    public UUID pathPattern(@PathParameter UUID uuid) {
        return uuid;
    }

    @Route(method = HttpMethod.GET, uri = "error")
    public void errorTest() {
        throw new IllegalStateException("I failed");
    }

    @Route(method = HttpMethod.GET, uri = "intercepted")
    @ContentType(MimeTypes.HTML)
    public String intercepted() {
        return "Foo";
    }

    @Route(method = HttpMethod.GET, uri = "lenna.png")
    public Promise<Result<Payload>> lenna() throws Exception {
        return reactiveResourceReader.readResource(HelloWorldController.class, "Lenna.png")
                .map(rf -> {
                    return ImmutableResult
                            .ok(Payload.ofLength(rf.length(), rf.autoCloseContent()))
                            .contentType("image/png").build();
                });
    }

    @Route(method = HttpMethod.GET, uri = "ws")
    public Promise<Result<Payload>> ws(HttpRequest request, @Body Payload payload) {
        UnicastPublisher<WebSocketMessage<Object>> publisher = webSocketManager.publisher();
        WebSocket<Object> webSocket = webSocketManager.createWebSocket(request, payload, publisher,
                TypeTag.of(Object.class));

        Flux.from(webSocket.incoming()).doOnNext(message -> System.out.println(message)).subscribe();

        return webSocket.result();
    }
}
