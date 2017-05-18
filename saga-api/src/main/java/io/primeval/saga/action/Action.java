package io.primeval.saga.action;

public final class Action {

    public final ActionKey actionKey;

    public final ActionFunction function;

    public Action(ActionKey actionKey, ActionFunction function) {
        this.actionKey = actionKey;
        this.function = function;
    }

    @Override
    public String toString() {
        return "Action [actionKey=" + actionKey.repr() + "]";
    }

}
