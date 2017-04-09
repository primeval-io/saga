package io.primeval.saga.websocket.message;

public final class WebSocketObjectMessage<T> extends WebSocketMessage<T> {

    public final T object;

    WebSocketObjectMessage(T object) {
        this.object = object;
    }

    @Override
    public <R> R visit(io.primeval.saga.websocket.message.WebSocketMessage.FunVisitor<T, R> visitor) {
        return visitor.visitObject(this.object);
    }
    
    @Override
    public void visit(io.primeval.saga.websocket.message.WebSocketMessage.Visitor<T> visitor) {
        visitor.visitObject(this.object);
    }

}
