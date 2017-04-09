package io.primeval.saga.core.test.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;

import org.junit.Test;
import org.osgi.util.promise.Promise;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;

import io.primeval.common.bytebuffer.ByteBufferListInputStream;
import io.primeval.saga.http.client.BoundHttpClient;
import io.primeval.saga.http.client.HttpClient;
import io.primeval.saga.http.client.HttpClientObjectResponse;
import io.primeval.saga.http.client.HttpClientRawResponse;
import io.primeval.saga.http.protocol.Status;
import io.primeval.saga.renderer.MimeTypes;
import reactor.core.publisher.Flux;

public abstract class AbstractHttpTestSuite {

    public abstract HttpClient client();

    public abstract int serverPort();

    @Test
    public void shouldGetBytes() throws InvocationTargetException, InterruptedException, IOException {
        BoundHttpClient boundHttpClient = client().as("Test-Client").to("localhost", serverPort());

        Promise<HttpClientRawResponse> promisedResult = boundHttpClient.get("/simpleGet").withHeader("foo", "bar").exec();

        HttpClientRawResponse response = promisedResult.getValue();
        assertThat(response.code).isEqualTo(Status.OK);

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                new ByteBufferListInputStream(Flux.from(response.payload.content).collectList().block()), Charsets.UTF_8))) {
            String collect = bufferedReader.lines().collect(Collectors.joining("\n"));
            assertThat(collect).isEqualTo("\"Hello World\"");
            assertThat(response.headers).containsKey("X-Test");
            assertThat(response.headers.get("X-Test")).contains("Foobar");
        }
    }

    @Test
    public void shouldGetObject() throws InvocationTargetException, InterruptedException {
        BoundHttpClient boundHttpClient = client().as("Test-Client").to("localhost", serverPort());

        Promise<HttpClientObjectResponse<String>> promisedResult = boundHttpClient.get("/simpleGet").withHeader("foo", "bar")
                .exec(String.class);
        HttpClientObjectResponse<String> response = promisedResult.getValue();
        assertThat(response.code).isEqualTo(Status.OK);
        assertThat(response.object).isEqualTo("Hello World");
        assertThat(response.headers).containsKey("X-Test");
        assertThat(response.headers.get("X-Test")).contains("Foobar");
    }

    @Test
    public void shouldUseHttpParameter() throws InvocationTargetException, InterruptedException {
        BoundHttpClient boundHttpClient = client().as("Test-Client").to("localhost", serverPort());

        Promise<HttpClientObjectResponse<String>> promisedResult = boundHttpClient
                .get(Multimaps.asMap(ImmutableListMultimap.of("who", "world")), "/hello")
                .exec(String.class);
        HttpClientObjectResponse<String> response = promisedResult.getValue();
        assertThat(response.code).isEqualTo(Status.OK);
        assertThat(response.object).isEqualTo("Hello world");
    }

    @Test
    public void shouldPostObject() throws InvocationTargetException, InterruptedException {
        BoundHttpClient boundHttpClient = client().as("Test-Client").withContentType(MimeTypes.JSON).to("localhost", serverPort());

        Promise<HttpClientObjectResponse<String>> promisedResult = boundHttpClient.post("/uppercase").withBody("hello!")
                .exec(String.class);

        // List<Promise<HttpClientObjectResponse<String>>> promisedResults = Lists.newArrayList();
        // for (int i = 0; i < 1; i++) {
        // Promise<HttpClientObjectResponse<String>> promisedResult2 =
        // boundHttpClient.post("/uppercase").withJsonBody("hello!")
        // .exec(String.class);
        // promisedResults.add(promisedResult2);
        // }
        // Promise<List<HttpClientObjectResponse<String>>> all = Promises.all(promisedResults);
        // all.getValue();

        HttpClientObjectResponse<String> response = promisedResult.getValue();
        assertThat(response.code).isEqualTo(Status.OK);
        assertThat(response.object).isEqualTo("HELLO!");
    }

    @Test
    public void shouldRetrievePlainText() throws InvocationTargetException, InterruptedException {
        BoundHttpClient boundHttpClient = client().as("Test-Client").to("localhost", serverPort());

        Promise<HttpClientObjectResponse<String>> promisedResult = boundHttpClient.get("/plainText")
                .exec(String.class);

        HttpClientObjectResponse<String> response = promisedResult.getValue();
        assertThat(response.code).isEqualTo(Status.OK);
        assertThat(response.object).isEqualTo("HELLO!");
    }

    @Test
    public void shouldDealWithUnknownRoutes() throws InvocationTargetException, InterruptedException {
        BoundHttpClient boundHttpClient = client().as("Test-Client").to("localhost", serverPort());

        Promise<HttpClientRawResponse> promisedResult = boundHttpClient.get("/doesntExist").withHeader("Accept", "application/json")
                .withHeader("Content-Type", "application/json").exec();

        HttpClientRawResponse resp = promisedResult.getValue();
        assertThat(resp.code).isEqualTo(404);
    }

    @Test
    public void shouldDealObjectsWithUnknownRoutes() throws InvocationTargetException, InterruptedException {
        BoundHttpClient boundHttpClient = client().as("Test-Client").to("localhost", serverPort());

        Promise<HttpClientObjectResponse<String>> promisedResult = boundHttpClient.get("/doesntExist").exec(String.class);

        Throwable resp = promisedResult.getFailure();
        assertThat(resp).isNotNull();
    }

    // @Test
    // public void shouldSendExecutionContext() throws InvocationTargetException, InterruptedException {
    //
    // ExecutionContext executionContext = wCodex.getExecutionContext();
    // executionContext.setCurrent(TestContextAttribute.class, new TestContextAttribute("foobarioz"));
    //
    // BoundHttpClient boundHttpClient = client().as("Test-Client").to("localhost", serverPort());
    //
    // Promise<HttpClientRawResponse> promisedResult = boundHttpClient.get("/checkExecutionContext").withHeader("foo",
    // "bar").exec();
    //
    // // Assert the promise resolution has access to the executionContext
    // Promise<Boolean> promiseGoodContext = promisedResult.map(r -> {
    // TestContextAttribute testContextAttribute = executionContext.getCurrent(TestContextAttribute.class);
    // if (testContextAttribute != null && testContextAttribute.foo.equals("foobarioz")) {
    // return true;
    // }
    // return false;
    // });
    // assertThat(promiseGoodContext.getValue()).isEqualTo(Boolean.TRUE);
    // assertThat(promisedResult.isDone()).isTrue();
    // HttpClientRawResponse response = promisedResult.getValue();
    //
    // assertThat(response.code).isEqualTo(HttpStatus.OK);
    // assertThat(new String(response.payload, Charsets.UTF_8)).isEqualTo("true");
    //
    // }

}
