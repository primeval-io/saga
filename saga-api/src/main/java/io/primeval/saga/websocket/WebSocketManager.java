package io.primeval.saga.websocket;

import java.util.Optional;

import org.reactivestreams.Publisher;

import io.primeval.codex.publisher.UnicastPublisher;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.http.protocol.HttpRequest;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.websocket.message.WebSocketMessage;

public interface WebSocketManager {

    <T> Optional<WebSocket<T>> tryToCreateWebSocket(HttpRequest request, Payload incomingPayload,
            Publisher<WebSocketMessage<T>> messagePublisher, TypeTag<? extends T> typeTag);

    default <T> WebSocket<T> createWebSocket(HttpRequest request, Payload incomingPayload,
            Publisher<WebSocketMessage<T>> messagePublisher, TypeTag<? extends T> typeTag) {
        return tryToCreateWebSocket(request, incomingPayload, messagePublisher, typeTag).orElseGet(() -> WebSocket.badWebSocketRequest());
    }

    default <T> UnicastPublisher<WebSocketMessage<T>> publisher() {
        return new UnicastPublisher<>();
    }
}
