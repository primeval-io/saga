package io.primeval.saga.websocket;

import java.util.Collections;
import java.util.NoSuchElementException;

import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.reactivestreams.Publisher;

import io.primeval.saga.action.Result;
import io.primeval.saga.http.protocol.Status;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.websocket.message.WebSocketMessage;
import reactor.core.publisher.Flux;

public interface WebSocket<T> {

    public static final Promise<Result<Payload>> BAD_REQUEST = Promises
            .resolved(new Result<>(Status.BAD_REQUEST, Collections.emptyMap(), Payload.EMPTY));
    public final static WebSocket<Object> BAD_WEBSOCKET_REQUEST = new WebSocket<Object>() {

        @Override
        public Promise<Result<Payload>> result() {
            return BAD_REQUEST;
        }

        @Override
        public Publisher<WebSocketMessage<Object>> incoming() {
            return Flux.error(new NoSuchElementException("not a valid websocket!"));
        }

    };

    Promise<Result<Payload>> result();

    Publisher<WebSocketMessage<T>> incoming();

    @SuppressWarnings("unchecked")
    public static <T> WebSocket<T> badWebSocketRequest() {
        return (WebSocket<T>) BAD_WEBSOCKET_REQUEST;
    }

}
