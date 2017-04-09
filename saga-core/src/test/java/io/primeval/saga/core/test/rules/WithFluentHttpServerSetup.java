package io.primeval.saga.core.test.rules;

import java.util.List;

import org.junit.rules.ExternalResource;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.primeval.common.test.rules.TestResource;
import net.codestory.http.WebServer;
import net.codestory.http.extensions.Extensions;
import net.codestory.http.misc.Env;
import net.codestory.http.payload.Payload;

public class WithFluentHttpServerSetup extends ExternalResource implements TestResource {

    private WebServer testHttpServer;
    private ObjectMapper mapper;

    @Override
    public void before() throws Throwable {
        mapper = new ObjectMapper();
        testHttpServer = new WebServer().configure(
                routes -> {
                    routes.setExtensions(new Extensions() {
                        @Override
                        public ObjectMapper configureOrReplaceObjectMapper(ObjectMapper defaultObjectMapper, Env env) {
                            return mapper;
                        }
                    });
                    routes.post("/uppercase", context -> {
                        String string = context.request().content();
                        return new Payload("application/json;charset=UTF-8", string.toUpperCase());
                    });
                    routes.get("/simpleGet", (context) -> {
                        return new Payload("application/json;charset=UTF-8",
                                mapper.writeValueAsString("Hello World"))
                                        .withHeader("X-Test", "Foobar");
                    });
                    routes.get("/checkExecutionContext", (context) -> {
                        List<String> headers = context.headers("X-ExecutionContext-Thingy");
                        if (headers != null && !headers.isEmpty()) {
                            return true;
                        }
                        return false;

                    });
                    routes.get("/plainText", context -> {
                        return new Payload("text/plain;charset=UTF-8", "HELLO!");
                    });
                    routes.get("/hello", context -> {
                        return new Payload("text/plain;charset=UTF-8", "Hello " + context.get("who"));
                    });
                }).startOnRandomPort();

    }

    @Override
    public void after() {
        testHttpServer.stop();
    }

    public int port() {
        return testHttpServer.port();
    }

}
