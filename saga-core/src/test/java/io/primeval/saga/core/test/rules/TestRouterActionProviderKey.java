package io.primeval.saga.core.test.rules;

import java.util.Objects;

import io.primeval.saga.router.spi.RouterActionProviderKey;

public final class TestRouterActionProviderKey extends RouterActionProviderKey {

    public final String name;

    public TestRouterActionProviderKey(String name) {
        this.name = name;
    }

    @Override
    public String repr() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (getClass() != o.getClass()) {
            return false;
        }
        TestRouterActionProviderKey other = (TestRouterActionProviderKey) o;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

}
