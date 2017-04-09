package io.primeval.saga.ninio.test.rules;

import org.junit.rules.ExternalResource;

import io.primeval.common.test.rules.TestResource;
import io.primeval.saga.ninio.internal.server.NinioHttpServerProvider;

public final class WithNinioHttpServerProvider extends ExternalResource implements TestResource {

    private NinioHttpServerProvider ninioHttpServerProvider;

    @Override
    public void before() throws Throwable {
        ninioHttpServerProvider = new NinioHttpServerProvider();
        ninioHttpServerProvider.activate();
    }

    @Override
    public void after() {
        ninioHttpServerProvider.deactivate();
    }

    public NinioHttpServerProvider getNinioHttpServerProvider() {
        return ninioHttpServerProvider;
    }

}
