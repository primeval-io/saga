package io.primeval.saga.router;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.osgi.util.promise.Promise;

import io.primeval.saga.http.protocol.HttpMethod;

public interface Router {

    Promise<Optional<RouterAction>> getActionFor(HttpMethod method, List<String> path);

    Promise<Optional<RouterAction>> getActionFor(String method, List<String> path);

    Promise<Collection<Route>> getRoutes();

}
