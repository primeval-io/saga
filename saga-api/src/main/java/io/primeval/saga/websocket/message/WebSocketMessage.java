package io.primeval.saga.websocket.message;

public abstract class WebSocketMessage<T> {

    public static <T> WebSocketObjectMessage<T> object(T object) {
        return new WebSocketObjectMessage<>(object);
    }

    public static <T> WebSocketTextMessage<T> text(String text) {
        return new WebSocketTextMessage<>(text);
    }

    public abstract void visit(Visitor<T> visitor);

    public abstract <R> R visit(FunVisitor<T, R> visitor);

    public interface Visitor<T> {
        void visitObject(T object);

        void visitText(String text);
    }

    public interface FunVisitor<T, R> {
        R visitObject(T object);

        R visitText(String text);
    }
}
