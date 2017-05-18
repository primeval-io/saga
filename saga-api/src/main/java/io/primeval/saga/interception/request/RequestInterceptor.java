package io.primeval.saga.interception.request;

import java.util.Optional;

import org.osgi.util.promise.Promise;

import io.primeval.saga.action.ActionFunction;
import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;
import io.primeval.saga.router.Route;

public interface RequestInterceptor {

    Promise<Result<?>> onRequest(Context context, ActionFunction function, Optional<Route> boundRoute);

    boolean matches(String uri);
}
