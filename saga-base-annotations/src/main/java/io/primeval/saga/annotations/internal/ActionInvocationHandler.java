package io.primeval.saga.annotations.internal;

import org.osgi.util.promise.Promise;

import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;

public interface ActionInvocationHandler {

    Promise<Result<?>> invoke(Context context);
}
