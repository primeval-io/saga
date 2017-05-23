package io.primeval.saga.interception.action;

import org.osgi.util.promise.Promise;

import io.primeval.saga.action.ActionFunction;
import io.primeval.saga.action.ActionKey;
import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;

public interface ActionInterceptor<T> {

    Promise<Result<?>> onAction(T attachment, Context context, ActionKey actionKey, ActionFunction action);

    Class<T> type();

    default boolean applyRecovery() {
        return true;
    }

    default ActionFunction wrap(ActionFunction fun, T attachment, ActionKey actionKey) {
        return wrap(this, fun, attachment, actionKey);
    }

    public static <T> ActionFunction wrap(ActionInterceptor<T> handler, ActionFunction fun, T attachment, ActionKey actionKey) {
        return context -> {
            return handler.onAction(attachment, context, actionKey, fun);
        };
    }
}
