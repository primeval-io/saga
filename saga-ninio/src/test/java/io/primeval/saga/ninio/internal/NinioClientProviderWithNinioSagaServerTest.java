package io.primeval.saga.ninio.internal;

import java.util.Collections;
import java.util.OptionalInt;

import org.junit.ClassRule;
import org.junit.rules.RuleChain;

import io.primeval.codex.test.rules.WithCodex;
import io.primeval.json.jackson.test.rules.WithJacksonMapper;
import io.primeval.saga.core.test.rules.AbstractHttpTestSuite;
import io.primeval.saga.core.test.rules.WithHttpClient;
import io.primeval.saga.core.test.rules.WithHttpServer;
import io.primeval.saga.core.test.rules.WithReactiveJsonSerDes;
import io.primeval.saga.core.test.rules.WithRouter;
import io.primeval.saga.core.test.rules.WithSagaHttpServerSetup;
import io.primeval.saga.core.test.rules.WithSerDes;
import io.primeval.saga.http.client.HttpClient;
import io.primeval.saga.ninio.test.rules.WithNinioHttpClientProvider;
import io.primeval.saga.ninio.test.rules.WithNinioHttpServerProvider;

public final class NinioClientProviderWithNinioSagaServerTest extends AbstractHttpTestSuite {

    public static final WithCodex wCodex = new WithCodex();
    public static final WithJacksonMapper wJacksonMapper = new WithJacksonMapper(Collections.emptyList());
    public static final WithReactiveJsonSerDes wJsonSerDes = new WithReactiveJsonSerDes(wJacksonMapper);
    public static final WithSerDes wSerDes = new WithSerDes(wJsonSerDes::getJsonReactiveDeserializer,
            wJsonSerDes::getJsonReactiveSerializer);

    public static final WithNinioHttpServerProvider wNinioHttpServerProvider = new WithNinioHttpServerProvider();
    public static final WithRouter wRouter = new WithRouter();
    public static final WithHttpServer wHttpServer = new WithHttpServer(wSerDes, wCodex, wRouter::getRouter,
            OptionalInt.empty(),
            wNinioHttpServerProvider::getNinioHttpServerProvider);

    public static final WithNinioHttpClientProvider wNinioHttpClientProvider = new WithNinioHttpClientProvider();
    public static final WithHttpClient wHttpClient = new WithHttpClient(wSerDes, wCodex,
            wNinioHttpClientProvider::getNinioHttpClientProvider);

    public static final WithSagaHttpServerSetup wHttpServerSetup = new WithSagaHttpServerSetup(wRouter);

    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(wCodex)
            .around(wJacksonMapper)
            .around(wJsonSerDes)
            .around(wSerDes)
            .around(wNinioHttpServerProvider)
            .around(wRouter)
            .around(wHttpServerSetup)
            .around(wHttpServer)
            .around(wNinioHttpClientProvider)
            .around(wHttpClient);

    @Override
    public HttpClient client() {
        return wHttpClient.getHttpClient();
    }

    @Override
    public int serverPort() {
        return wHttpServer.port();
    }

}
