package io.primeval.saga.router.exception;

import java.util.Optional;

import org.osgi.util.promise.Promise;

import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;
import io.primeval.saga.router.Route;

@FunctionalInterface
public interface ExceptionRecovery<T extends Throwable> {
    Promise<Result<?>> recover(T exception, Context context, Optional<Route> boundRoute);

}
