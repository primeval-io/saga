package io.primeval.saga.core.test.rules;

import java.util.Objects;

import io.primeval.saga.action.ActionKey;

public final class TestActionKey extends ActionKey {

    public final String name;

    public TestActionKey(String name) {
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
        TestActionKey other = (TestActionKey) o;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public ClassLoader classLoader() {
        return TestActionKey.class.getClassLoader();
    }
}
