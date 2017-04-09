package io.primeval.saga.ninio.internal;

import java.util.Collections;
import java.util.OptionalInt;

import com.davfx.ninio.util.Wait;
import com.google.common.collect.ImmutableList;

import io.primeval.codex.test.rules.WithCodex;
import io.primeval.common.test.rules.TestResource;
import io.primeval.json.jackson.test.rules.WithJacksonMapper;
import io.primeval.saga.core.test.rules.WithHttpClient;
import io.primeval.saga.core.test.rules.WithHttpServer;
import io.primeval.saga.core.test.rules.WithReactiveJsonSerDes;
import io.primeval.saga.core.test.rules.WithRouter;
import io.primeval.saga.core.test.rules.WithSagaAnnotatedHttpServerSetup;
import io.primeval.saga.core.test.rules.WithSerDes;
import io.primeval.saga.core.test.rules.WithTestController;
import io.primeval.saga.core.test.rules.WithWebSocketManager;
import io.primeval.saga.ninio.test.rules.WithNinioHttpClientProvider;
import io.primeval.saga.ninio.test.rules.WithNinioHttpServerProvider;

public final class NinioClientProviderWithNinioAnnotatedSagaServerRunner {

    public static final WithCodex wCodex = new WithCodex();
    public static final WithJacksonMapper wJacksonMapper = new WithJacksonMapper(Collections.emptyList());
    public static final WithReactiveJsonSerDes wJsonSerDes = new WithReactiveJsonSerDes(wJacksonMapper);
    public static final WithSerDes wSerDes = new WithSerDes(wJsonSerDes::getJsonReactiveDeserializer,
            wJsonSerDes::getJsonReactiveSerializer);


    public static final WithNinioHttpServerProvider wNinioHttpServerProvider = new WithNinioHttpServerProvider();
    public static final WithRouter wRouter = new WithRouter();
    public static final WithWebSocketManager wWSManager = new WithWebSocketManager(wSerDes);

    public static final WithHttpServer wHttpServer = new WithHttpServer(wSerDes, wCodex, wRouter::getRouter,
            OptionalInt.of(9001),
            wNinioHttpServerProvider::getNinioHttpServerProvider);

    public static final WithNinioHttpClientProvider wNinioHttpClientProvider = new WithNinioHttpClientProvider();
    public static final WithHttpClient wHttpClient = new WithHttpClient(wSerDes, wCodex,
            wNinioHttpClientProvider::getNinioHttpClientProvider);

    public static final WithTestController wTestController = new WithTestController(wWSManager, wCodex);
    public static final WithSagaAnnotatedHttpServerSetup wHttpServerSetup = new WithSagaAnnotatedHttpServerSetup(
            wRouter, wTestController);

    public static void main(String[] args) throws Throwable {
        ImmutableList<TestResource> resources = ImmutableList.of(wCodex, wJacksonMapper, wJsonSerDes, wSerDes, wNinioHttpServerProvider,
                wRouter, wWSManager, wTestController, wHttpServerSetup, wHttpServer,
                wNinioHttpClientProvider, wHttpClient);
        for (TestResource res : resources) {
            res.before();
        }
        new Wait().waitFor();
        for (TestResource res : resources) {
            res.after();
        }
    }

}
