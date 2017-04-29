package io.primeval.saga.annotations.internal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

import com.google.common.collect.ImmutableList;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;
import io.primeval.saga.http.protocol.HttpRequest;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.router.RouterAction;

public class ControllerRouteFinderTest {

    ControllerRouteFinder tested = new ControllerRouteFinder();

    @Test
    public void test() throws Exception {
        HelloWorldController helloWorldController = new HelloWorldController();

        Collection<RouterAction> routerActions = tested.routerActions(helloWorldController);

        for (RouterAction ba : routerActions) {

            Result<?> result = ba.action.function.apply(new Context() {

                @Override
                public <T> Promise<T> queryParameter(String parameterName, TypeTag<? extends T> typeTag,
                        ClassLoader classLoader) {
                    if (typeTag.rawType() == Optional.class) {
                        return (Promise<T>) Promises.resolved(Optional.of("foo"));
                    }
                    return (Promise<T>) Promises.resolved("foo");
                }

                @Override
                public List<Optional<String>> queryParameter(String parameterName) {
                    return ImmutableList.of(Optional.of("foo"));
                }

                @Override
                public <T> Promise<T> body(TypeTag<? extends T> typeTag, ClassLoader classLoader) {
                    return (Promise<T>) Promises.resolved("Hello World");
                }

                @Override
                public HttpRequest request() {
                    return null;
                }

                @Override
                public Payload body() {
                    return null;
                }
            }).getValue();

            System.out.println(ba.action.actionKey.repr());
            System.out.println(result.statusCode() + " " + result.content().get().value());

        }

    }

}
