package io.primeval.saga.websocket.message;

public final class WebSocketTextMessage<T> extends WebSocketMessage<T> {

    public final String text;

    WebSocketTextMessage(String text) {
        this.text = text;
    }

    @Override
    public <R> R visit(io.primeval.saga.websocket.message.WebSocketMessage.FunVisitor<T, R> visitor) {
        return visitor.visitText(this.text);
    }

    @Override
    public void visit(io.primeval.saga.websocket.message.WebSocketMessage.Visitor<T> visitor) {
        visitor.visitText(this.text);
    }

}
