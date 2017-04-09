package io.primeval.saga.core.test.rules;

import org.junit.rules.ExternalResource;

import io.primeval.common.test.rules.TestResource;
import io.primeval.saga.core.internal.server.websocket.WebSocketManagerImpl;

public class WithWebSocketManager extends ExternalResource implements TestResource {

    private WebSocketManagerImpl webSocketManager;
    private WithSerDes wSerDes;

    public WithWebSocketManager(WithSerDes wSerDes) {
        this.wSerDes = wSerDes;
    }

    @Override
    public void before() throws Throwable {
        webSocketManager = new WebSocketManagerImpl();
        webSocketManager.setSetDes(wSerDes.getSerDes());
    }

    public void after() {
    };

    public WebSocketManagerImpl getWebSocketManager() {
        return webSocketManager;
    }

}
