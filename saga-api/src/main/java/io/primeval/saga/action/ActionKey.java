package io.primeval.saga.action;

public abstract class ActionKey {

    public abstract String repr();

    public abstract ClassLoader classLoader();

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();
}
