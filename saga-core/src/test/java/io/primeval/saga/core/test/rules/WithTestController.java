package io.primeval.saga.core.test.rules;

import org.junit.rules.ExternalResource;

import io.primeval.codex.test.rules.WithCodex;
import io.primeval.common.test.rules.TestResource;

public class WithTestController extends ExternalResource implements TestResource {

    private TestController testController;
    private WithCodex wCodex;
    private WithWebSocketManager wWSManager;

    public WithTestController(WithWebSocketManager wWSManager, WithCodex wCodex) {
        this.wWSManager = wWSManager;
        this.wCodex = wCodex;
    }

    @Override
    public void before() throws Throwable {
        testController = new TestController();
        testController.setScheduler(wCodex.getScheduler());
        testController.setWebSocketManager(wWSManager.getWebSocketManager());
    }

    @Override
    public void after() {
    }

    public TestController getTestController() {
        return testController;
    }
}
