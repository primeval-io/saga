package io.primeval.saga.action;

import java.util.function.Function;

import org.osgi.util.promise.Promise;

import io.primeval.common.type.TypeTag;

public final class Action {

    public final Function<Context, Promise<Result<?>>> function;

    public final TypeTag actionType; // the typeTag *in* Result.

    public final ActionKey actionKey;

    public Action(Function<Context, Promise<Result<?>>> function, TypeTag<?> actionType, ActionKey actionKey) {
        this.actionKey = actionKey;
        this.function = function;
        this.actionType = actionType;
    }

}
