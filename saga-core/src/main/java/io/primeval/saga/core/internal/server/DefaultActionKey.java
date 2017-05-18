package io.primeval.saga.core.internal.server;

import java.util.Objects;

import io.primeval.saga.action.ActionKey;

public final class DefaultActionKey extends ActionKey {

    public final String name;

    public DefaultActionKey(String name) {
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
        DefaultActionKey other = (DefaultActionKey) o;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    @Override
    public ClassLoader classLoader() {
        return DefaultActionKey.class.getClassLoader();
    }

}
