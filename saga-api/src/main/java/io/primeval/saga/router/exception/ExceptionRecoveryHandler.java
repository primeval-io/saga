package io.primeval.saga.router.exception;

import java.util.Optional;

import org.osgi.util.promise.Promise;

import io.primeval.codex.promise.PromiseHelper;
import io.primeval.saga.action.ActionFunction;
import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;
import io.primeval.saga.router.RequestHandler;
import io.primeval.saga.router.Route;

public interface ExceptionRecoveryHandler extends RequestHandler {


    <T extends Throwable> Promise<Result<?>> handleRecovery(T failure, Context context,
            Optional<Route> boundRoute);
    
    default Promise<Result<?>> onRequest(Context context, ActionFunction function, Optional<Route> boundRoute) {
        return PromiseHelper.wrapPromise(() -> function.apply(context))
                .recoverWith(p -> handleRecovery(PromiseHelper.getFailure(p), context, boundRoute));
    }

}
