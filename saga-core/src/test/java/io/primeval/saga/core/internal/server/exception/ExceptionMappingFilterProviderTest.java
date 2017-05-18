package io.primeval.saga.core.internal.server.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;
import io.primeval.saga.http.protocol.HttpHost;
import io.primeval.saga.http.protocol.HttpMethod;
import io.primeval.saga.http.protocol.HttpRequest;
import io.primeval.saga.http.protocol.Status;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.router.Route;
import io.primeval.saga.router.exception.ExceptionRecoveryProvider;

public class ExceptionMappingFilterProviderTest {

    private static ExceptionMappingFilterProvider tested;
    private static Context fakeContext;

    @BeforeClass
    public static void setUp() {
        tested = new ExceptionMappingFilterProvider();
        fakeContext = new Context() {

            @Override
            public HttpRequest request() {
                return new HttpRequest(new HttpHost("http", "localhost", 80), HttpMethod.GET, "/foo",
                        ImmutableList.of("foo"), Collections.emptyMap(), Collections.emptyMap());
            }

            @Override
            public <T> Promise<T> queryParameter(String parameterName, TypeTag<? extends T> typeTag,
                    ClassLoader classLoader) {
                return null;
            }

            @Override
            public List<Optional<String>> queryParameter(String parameterName) {
                return null;
            }

            @Override
            public <T> Promise<T> body(TypeTag<? extends T> typeTag, ClassLoader classLoader) {
                return null;
            }

            @Override
            public Payload body() {
                return null;
            }
        };
        addMapper(0, new ExceptionRecoveryProvider<Throwable>() {

            @Override
            public Promise<Result<?>> recover(Throwable exception, Context context,
                    Optional<Route> boundRoute) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                exception.printStackTrace(pw);

                return Promises.resolved(Result.create(Status.INTERNAL_SERVER_ERROR,
                        "Internal server error on URI " + context.request().uri + ": " + sw.toString()));
            }

            @Override
            public Class<Throwable> exceptionType() {
                return Throwable.class;
            }
        });

        addMapper(0, new ExceptionRecoveryProvider<RuntimeException>() {

            @Override
            public Promise<Result<?>> recover(RuntimeException exception, Context context,
                    Optional<Route> boundRoute) {
                throw new NoSuchElementException();
            }

            @Override
            public Class<RuntimeException> exceptionType() {
                return RuntimeException.class;
            }
        });

        addMapper(0, new ExceptionRecoveryProvider<NoSuchElementException>() {

            @Override
            public Promise<Result<?>> recover(NoSuchElementException exception, Context context,
                    Optional<Route> boundRoute) {
                return null;
            }

            @Override
            public Class<NoSuchElementException> exceptionType() {
                return NoSuchElementException.class;
            }
        });

        addMapper(1, new ExceptionRecoveryProvider<NoSuchElementException>() {

            @Override
            public Promise<Result<?>> recover(NoSuchElementException exception, Context context,
                    Optional<Route> boundRoute) {
                throw new NoSuchElementException(exception.getMessage() + "!");
            }

            @Override
            public Class<NoSuchElementException> exceptionType() {
                return NoSuchElementException.class;
            }
        });

        addMapper(2, new ExceptionRecoveryProvider<NoSuchElementException>() {

            @Override
            public Promise<Result<?>> recover(NoSuchElementException exception, Context context,
                    Optional<Route> boundRoute) {
                return Promises.resolved(Result.create(Status.NOT_FOUND, exception.getMessage()));
            }

            @Override
            public Class<NoSuchElementException> exceptionType() {
                return NoSuchElementException.class;
            }
        });
    }

    private static void addMapper(int pos, ExceptionRecoveryProvider<?> exceptionMapper) {
        tested.addExceptionMapper(exceptionMapper, new TestOrderer<ExceptionRecoveryProvider<?>>(exceptionMapper, pos));
    }

    @Test
    public void testRecovery() throws Exception {

        Promise<Result<?>> promise = tested.onRequest(fakeContext,
                ctx -> Promises.failed(new NoSuchElementException("Not found")),
                Optional.empty());

        Result<?> result = promise.getValue();
        Assertions.assertThat(result.statusCode()).isEqualTo(404);
        Assertions.assertThat(result.content().get().value()).isEqualTo("Not found!");

    }

    @Test
    public void testRecoveryInheritance() throws Exception {
        Promise<Result<?>> promise = tested.onRequest(fakeContext,
                ctx -> Promises.failed(new NoSuchFruitException("banana")),
                Optional.empty());

        Result<?> result = promise.getValue();
        Assertions.assertThat(result.statusCode()).isEqualTo(404);
        Assertions.assertThat(result.content().get().value()).isEqualTo("Missing fruit: banana!");
    }

    @Test
    public void testRethrow() throws Exception {
        Promise<Result<?>> promise = tested.onRequest(fakeContext, ctx -> Promises.failed(new RuntimeException("boom")),
                Optional.empty());

        Result<?> result = promise.getValue();
        Assertions.assertThat(result.statusCode()).isEqualTo(500);
        Assertions.assertThat(Splitter.on('\n').splitToList((String) result.content().get().value()).get(0))
                .isEqualTo("Internal server error on URI /foo: java.util.NoSuchElementException");
    }

}
