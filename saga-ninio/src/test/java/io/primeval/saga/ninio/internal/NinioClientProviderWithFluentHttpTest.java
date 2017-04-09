package io.primeval.saga.ninio.internal;

import java.util.Collections;

import org.junit.ClassRule;
import org.junit.rules.RuleChain;

import io.primeval.codex.test.rules.WithCodex;
import io.primeval.json.jackson.test.rules.WithJacksonMapper;
import io.primeval.saga.core.test.rules.AbstractHttpTestSuite;
import io.primeval.saga.core.test.rules.WithFluentHttpServerSetup;
import io.primeval.saga.core.test.rules.WithHttpClient;
import io.primeval.saga.core.test.rules.WithReactiveJsonSerDes;
import io.primeval.saga.core.test.rules.WithSerDes;
import io.primeval.saga.http.client.HttpClient;
import io.primeval.saga.ninio.test.rules.WithNinioHttpClientProvider;

public final class NinioClientProviderWithFluentHttpTest extends AbstractHttpTestSuite {

    public static final WithCodex wCodex = new WithCodex();
    public static final WithJacksonMapper wJacksonMapper = new WithJacksonMapper(Collections.emptyList());
    public static final WithReactiveJsonSerDes wJsonSerDes = new WithReactiveJsonSerDes(wJacksonMapper);
    public static final WithSerDes wSerDes = new WithSerDes(wJsonSerDes::getJsonReactiveDeserializer,
            wJsonSerDes::getJsonReactiveSerializer);

    public static final WithFluentHttpServerSetup wHttpServerSetup = new WithFluentHttpServerSetup();
    public static final WithNinioHttpClientProvider wNinioHttpClientProvider = new WithNinioHttpClientProvider();
    public static final WithHttpClient wHttpClient = new WithHttpClient(wSerDes, wCodex,
            wNinioHttpClientProvider::getNinioHttpClientProvider);

    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(wJacksonMapper).around(wJsonSerDes).around(wSerDes)
            .around(wHttpServerSetup)
            .around(wCodex)
            .around(wNinioHttpClientProvider)
            .around(wHttpClient);

    @Override
    public HttpClient client() {
        return wHttpClient.getHttpClient();
    }

    @Override
    public int serverPort() {
        return wHttpServerSetup.port();
    }

}
