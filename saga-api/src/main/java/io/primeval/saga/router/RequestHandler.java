package io.primeval.saga.router;

import java.util.Optional;

import org.osgi.util.promise.Promise;

import io.primeval.saga.action.ActionFunction;
import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;

public interface RequestHandler {

    Promise<Result<?>> onRequest(Context context, ActionFunction function, Optional<Route> boundRoute);

    default ActionFunction wrap(ActionFunction fun, Optional<Route> boundRoute) {
        return wrap(this, fun, boundRoute);
    }
    
    public static ActionFunction wrap(RequestHandler handler, ActionFunction fun, Optional<Route> boundRoute) {
        return context -> {
            return handler.onRequest(context, fun, boundRoute);
        };
    }
}
