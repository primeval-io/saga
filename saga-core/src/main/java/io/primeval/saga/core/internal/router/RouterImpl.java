package io.primeval.saga.core.internal.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.promise.Promise;

import io.primeval.codex.promise.PromiseHelper;
import io.primeval.saga.http.protocol.HttpMethod;
import io.primeval.saga.router.Route;
import io.primeval.saga.router.Router;
import io.primeval.saga.router.RouterAction;
import io.primeval.saga.router.spi.RouterActionProvider;
import io.primeval.saga.router.spi.RouterActionProviderKey;

@Component(immediate = true)
public final class RouterImpl implements Router {

    private final Map<RouterActionProviderKey, List<RouterAction>> boundActionByKey = new ConcurrentHashMap<>();;

    @Override
    public Promise<Optional<RouterAction>> getActionFor(HttpMethod method, List<String> path) {
        return PromiseHelper.wrap(() -> {
            // naive route finding
            for (List<RouterAction> boundActions : boundActionByKey.values()) {
                for (RouterAction boundAction : boundActions) {
                    if (boundAction.route.matches(method, path)) {
                        return Optional.of(boundAction);
                    }
                }
            }
            return Optional.empty();
        });
    }

    @Override
    public Promise<Optional<RouterAction>> getActionFor(String method, List<String> path) {
        return getActionFor(HttpMethod.from(method), path);
    }

    @Override
    public Promise<Collection<Route>> getRoutes() {
        return PromiseHelper.wrap(() -> {
            return boundActionByKey.values().stream().flatMap(e -> e.stream()).map(ra -> ra.route)
                    .collect(Collectors.toList());
        });
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addRouterActionProvider(RouterActionProvider provider) {
        boundActionByKey.put(provider.id(), new ArrayList<>(provider.routerActions()));
    }

    public void removeRouterActionProvider(RouterActionProvider provider) {
        boundActionByKey.remove(provider.id());
    }

}
