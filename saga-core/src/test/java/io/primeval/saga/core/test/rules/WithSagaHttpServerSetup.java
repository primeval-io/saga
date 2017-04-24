package io.primeval.saga.core.test.rules;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

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
import io.primeval.saga.action.ActionFunction;
import io.primeval.saga.core.internal.router.RouterImpl;
import io.primeval.saga.guava.ImmutableResult;
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

        ActionFunction upperCase = context -> {
            Promise<String> bodyPms = context.body(String.class);
            return bodyPms.map(s -> ImmutableResult.ok(s.toUpperCase()).contentType(MimeTypes.JSON).build());
        };
        router.addRouterActionProvider(providerFor(Route.create(HttpMethod.POST, ImmutableList.of("uppercase")),
                upperCase));

        ActionFunction simpleGet = context -> {
            return Promises
                    .resolved(ImmutableResult.ok("Hello World").contentType(MimeTypes.JSON)
                            .withHeader("X-Test", "Foobar").build());
        };
        router.addRouterActionProvider(providerFor(Route.create(HttpMethod.GET, ImmutableList.of("simpleGet")),
                simpleGet));

        ActionFunction plainText = context -> {
            return Promises.resolved(ImmutableResult.ok("HELLO!").contentType(MimeTypes.TEXT).build());
        };

        router.addRouterActionProvider(providerFor(Route.create(HttpMethod.GET, ImmutableList.of("plainText")),
                plainText));

        ActionFunction hello = context -> {
            return context.queryParameter("who", new TypeTag<Optional<String>>() {
            }).map(param -> param.orElse("unknown user"))
                    .map(p -> ImmutableResult.ok("Hello " + p).contentType(MimeTypes.TEXT).build());
        };

        router.addRouterActionProvider(providerFor(Route.create(HttpMethod.GET, ImmutableList.of("hello")),
                hello));

    }

    private static RouterActionProvider providerFor(Route route, ActionFunction function) {
        return new RouterActionProvider() {
            String key = "test." + route.method.name() + Joiner.on('/').join(route.pathPattern);

            @Override
            public Collection<RouterAction> routerActions() {
                return Collections.singleton(new RouterAction(route, new Action(new TestActionKey(key), function)));
            }

            @Override
            public RouterActionProviderKey id() {
                return new TestRouterActionProviderKey(key);
            }
        };

    }

}
