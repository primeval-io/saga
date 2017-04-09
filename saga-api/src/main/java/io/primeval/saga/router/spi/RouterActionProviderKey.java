package io.primeval.saga.router.spi;

public abstract class RouterActionProviderKey {

    public abstract String repr();

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();
}
