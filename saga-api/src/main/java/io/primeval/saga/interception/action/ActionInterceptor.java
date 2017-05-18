package io.primeval.saga.interception.action;

import org.osgi.util.promise.Promise;

import io.primeval.saga.action.ActionFunction;
import io.primeval.saga.action.ActionKey;
import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;

public interface ActionInterceptor<T> {
    
    Promise<Result<?>> onAction(T attachment, Context context, ActionKey actionKey, ActionFunction action);

    Class<T> type();

}
