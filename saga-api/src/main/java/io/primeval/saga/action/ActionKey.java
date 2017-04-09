package io.primeval.saga.action;

public abstract class ActionKey {

    public abstract String repr();

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();
}
