package io.primeval.saga.core.test.rules;

import java.util.Collection;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.primeval.common.test.rules.TestResource;
import io.primeval.saga.annotations.internal.ControllerRouteFinder;
import io.primeval.saga.core.internal.router.RouterImpl;
import io.primeval.saga.router.RouterAction;
import io.primeval.saga.router.spi.RouterActionProvider;
import io.primeval.saga.router.spi.RouterActionProviderKey;

public class WithSagaAnnotatedHttpServerSetup extends ExternalResource implements TestResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(WithSagaAnnotatedHttpServerSetup.class);

    private WithRouter wRouter;
    private WithTestController withTestController;

    public WithSagaAnnotatedHttpServerSetup(WithRouter wRouter, WithTestController withTestController) {
        this.wRouter = wRouter;
        this.withTestController = withTestController;
    }

    @Override
    public void before() throws Throwable {
        setupRoutes(wRouter.getRouter(), withTestController.getTestController());
    }

    @Override
    public void after() {
    }

    static void setupRoutes(RouterImpl router, TestController testController) {

        ControllerRouteFinder sagaRouteFinder = new ControllerRouteFinder();

        Collection<RouterAction> routerActions = sagaRouteFinder.routerActions(testController);

        router.addRouterActionProvider(new RouterActionProvider() {

            @Override
            public Collection<RouterAction> routerActions() {
                return routerActions;
            }

            @Override
            public RouterActionProviderKey id() {
                return new TestRouterActionProviderKey("test");
            }
        });

    }

}
