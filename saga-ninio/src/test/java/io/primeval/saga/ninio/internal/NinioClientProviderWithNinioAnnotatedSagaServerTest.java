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
import io.primeval.saga.core.test.rules.WithSagaAnnotatedHttpServerSetup;
import io.primeval.saga.core.test.rules.WithSerDes;
import io.primeval.saga.core.test.rules.WithTestController;
import io.primeval.saga.core.test.rules.WithWebSocketManager;
import io.primeval.saga.http.client.HttpClient;
import io.primeval.saga.ninio.test.rules.WithNinioHttpClientProvider;
import io.primeval.saga.ninio.test.rules.WithNinioHttpServerProvider;

public final class NinioClientProviderWithNinioAnnotatedSagaServerTest extends AbstractHttpTestSuite {

    public static final WithCodex wCodex = new WithCodex();
    public static final WithJacksonMapper wJacksonMapper = new WithJacksonMapper(Collections.emptyList());
    public static final WithReactiveJsonSerDes wJsonSerDes = new WithReactiveJsonSerDes(wJacksonMapper);
    public static final WithSerDes wSerDes = new WithSerDes(wJsonSerDes::getJsonReactiveDeserializer,
            wJsonSerDes::getJsonReactiveSerializer);

    public static final WithNinioHttpServerProvider wNinioHttpServerProvider = new WithNinioHttpServerProvider();
    public static final WithRouter wRouter = new WithRouter();
    public static final WithWebSocketManager wWSManager = new WithWebSocketManager(wSerDes);

    public static final WithHttpServer wHttpServer = new WithHttpServer(wSerDes, wCodex, wRouter::getRouter,
            OptionalInt.empty(),
            wNinioHttpServerProvider::getNinioHttpServerProvider);

    public static final WithNinioHttpClientProvider wNinioHttpClientProvider = new WithNinioHttpClientProvider();
    public static final WithHttpClient wHttpClient = new WithHttpClient(wSerDes, wCodex,
            wNinioHttpClientProvider::getNinioHttpClientProvider);

    public static final WithTestController wTestController = new WithTestController(wWSManager, wCodex);
    public static final WithSagaAnnotatedHttpServerSetup wHttpServerSetup = new WithSagaAnnotatedHttpServerSetup(
            wRouter, wTestController);

    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(wCodex)
            .around(wJacksonMapper)
            .around(wJsonSerDes)
            .around(wSerDes)
            .around(wNinioHttpServerProvider)
            .around(wRouter)
            .around(wWSManager)
            .around(wTestController)
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
