package io.primeval.saga.core.test.rules;

import java.util.function.Supplier;

import org.junit.rules.ExternalResource;

import io.primeval.codex.test.rules.WithCodex;
import io.primeval.common.test.rules.TestResource;
import io.primeval.saga.core.internal.client.HttpClientImpl;
import io.primeval.saga.http.client.spi.HttpClientProvider;

public class WithHttpClient extends ExternalResource implements TestResource {

    private HttpClientImpl httpClient;
    private WithCodex wCodex;
    private WithSerDes wSerDes;
    private Supplier<HttpClientProvider> clientProvider;

    public WithHttpClient(WithSerDes wSerDes, WithCodex wCodex, Supplier<HttpClientProvider> clientProvider) {
        this.wSerDes = wSerDes;
        this.wCodex = wCodex;
        this.clientProvider = clientProvider;
    }

    @Override
    public void before() throws Throwable {
        super.before();
        httpClient = new HttpClientImpl();
        httpClient.setExecutionContextManager(wCodex.getExecutionContextManager());
        httpClient.setSerDes(wSerDes.getSerDes());

        httpClient.setDispatcher(wCodex.getDispatcher());
        httpClient.setScheduler(wCodex.getScheduler());

        httpClient.setHttpClientProvider(clientProvider.get());

    }

    @Override
    public void after() {
        super.after();
    }

    public HttpClientImpl getHttpClient() {
        return httpClient;
    }

}
