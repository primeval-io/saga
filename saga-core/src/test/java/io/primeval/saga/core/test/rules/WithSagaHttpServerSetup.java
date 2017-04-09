package io.primeval.saga.core.test.rules;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import org.junit.rules.ExternalResource;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import io.primeval.common.test.rules.TestResource;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.action.Action;
import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;
import io.primeval.saga.core.internal.router.RouterImpl;
import io.primeval.saga.http.protocol.HttpMethod;
import io.primeval.saga.renderer.MimeTypes;
import io.primeval.saga.router.Route;
import io.primeval.saga.router.RouterAction;
import io.primeval.saga.router.spi.RouterActionProvider;
import io.primeval.saga.router.spi.RouterActionProviderKey;

public class WithSagaHttpServerSetup extends ExternalResource implements TestResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(WithSagaHttpServerSetup.class);

    private WithRouter wRouter;

    public WithSagaHttpServerSetup(WithRouter wRouter) {
        this.wRouter = wRouter;
    }

    @Override
    public void before() throws Throwable {

        setupRoutes(wRouter.getRouter());

    }

    @Override
    public void after() {
    }

    static void setupRoutes(RouterImpl router) {

        Function<Context, Promise<Result<?>>> upperCase = context -> {
            Promise<String> bodyPms = context.body(String.class);
            return bodyPms.map(s -> Result.ok(s.toUpperCase()).contentType(MimeTypes.JSON));
        };
        router.addRouterActionProvider(providerFor(Route.create(HttpMethod.POST, ImmutableList.of("uppercase")),
                upperCase, TypeTag.of(String.class)));

        Function<Context, Promise<Result<?>>> simpleGet = context -> {
            return Promises
                    .resolved(Result.ok("Hello World").contentType(MimeTypes.JSON).withHeader("X-Test", "Foobar"));
        };
        router.addRouterActionProvider(providerFor(Route.create(HttpMethod.GET, ImmutableList.of("simpleGet")),
                simpleGet, TypeTag.of(String.class)));

        Function<Context, Promise<Result<?>>> plainText = context -> {
            return Promises.resolved(Result.ok("HELLO!").contentType(MimeTypes.TEXT));
        };

        router.addRouterActionProvider(providerFor(Route.create(HttpMethod.GET, ImmutableList.of("plainText")),
                plainText, TypeTag.of(String.class)));

        Function<Context, Promise<Result<?>>> hello = context -> {
            return context.queryParameter("who", new TypeTag<Optional<String>>() {
            }).map(param -> param.orElse("unknown user")).map(p -> Result.ok("Hello " + p).contentType(MimeTypes.TEXT));
        };

        router.addRouterActionProvider(providerFor(Route.create(HttpMethod.GET, ImmutableList.of("hello")),
                hello, TypeTag.of(String.class)));

    }

    private static RouterActionProvider providerFor(Route route, Function<Context, Promise<Result<?>>> function,
            TypeTag<String> actionType) {
        return new RouterActionProvider() {
            String key = "test." + route.method.name() + Joiner.on('/').join(route.pathPattern);

            @Override
            public Collection<RouterAction> routerActions() {
                return Collections.singleton(new RouterAction(route, new Action(function, actionType,
                        new TestActionKey(key))));
            }

            @Override
            public RouterActionProviderKey id() {
                return new TestRouterActionProviderKey(key);
            }
        };

    }

}
