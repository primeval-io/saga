package io.primeval.saga.core.test.rules;

import org.junit.rules.ExternalResource;

import io.primeval.common.test.rules.TestResource;
import io.primeval.saga.core.internal.router.RouterImpl;

public class WithRouter extends ExternalResource implements TestResource {

    private RouterImpl routerImpl;

    @Override
    public void before() throws Throwable {
        routerImpl = new RouterImpl();
    }

    @Override
    public void after() {
    }
    
    public RouterImpl getRouter() {
        return routerImpl;
    }

}
