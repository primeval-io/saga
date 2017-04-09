package io.primeval.saga.ninio.test.rules;

import org.junit.rules.ExternalResource;

import io.primeval.common.test.rules.TestResource;
import io.primeval.saga.ninio.internal.client.NinioHttpClientProvider;

public class WithNinioHttpClientProvider extends ExternalResource implements TestResource {

    private NinioHttpClientProvider ninioHttpClientProvider;

    @Override
    public void before() throws Throwable {
        super.before();
        ninioHttpClientProvider = new NinioHttpClientProvider();
        ninioHttpClientProvider.activate();

    }

    @Override
    public void after() {
        ninioHttpClientProvider.deactivate();
        super.after();
    }

    public NinioHttpClientProvider getNinioHttpClientProvider() {
        return ninioHttpClientProvider;
    }

}
