package io.primeval.saga.examples.helloworld;

import java.util.List;
import java.util.NoSuchElementException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;

import com.google.common.collect.ImmutableList;

import io.primeval.codex.publisher.UnicastPublisher;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.action.Result;
import io.primeval.saga.annotations.Body;
import io.primeval.saga.annotations.QueryParameter;
import io.primeval.saga.annotations.Route;
import io.primeval.saga.controller.Controller;
import io.primeval.saga.http.protocol.HttpMethod;
import io.primeval.saga.http.protocol.HttpRequest;
import io.primeval.saga.http.protocol.Status;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.websocket.WebSocket;
import io.primeval.saga.websocket.WebSocketManager;
import io.primeval.saga.websocket.message.WebSocketMessage;
import reactor.core.publisher.Flux;

@Component
@Controller
public final class HelloWorldController {

    @Reference
    private WebSocketManager webSocketManager;

    @Route(method = HttpMethod.GET, uri = "hello")
    public String hello(@QueryParameter String who) {
        return "Hello " + who;
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

    @Route(method = HttpMethod.GET, uri = "ws")
    public Promise<Result<Payload>> ws(HttpRequest request, @Body Payload payload) {
        UnicastPublisher<WebSocketMessage<Object>> publisher = webSocketManager.publisher();
        WebSocket<Object> webSocket = webSocketManager.createWebSocket(request, payload, publisher,
                TypeTag.of(Object.class));

        Flux.from(webSocket.incoming()).doOnNext(message -> System.out.println(message)).subscribe();

        return webSocket.result();
    }
}
