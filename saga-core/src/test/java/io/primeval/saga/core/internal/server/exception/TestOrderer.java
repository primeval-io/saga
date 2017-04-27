package io.primeval.saga.core.internal.server.exception;

import io.primeval.saga.core.internal.server.Orderer;

public final class TestOrderer<T> implements Orderer<T> {

    private final T element;
    private final int pos;

    public TestOrderer(T element, int pos) {
        this.element = element;
        this.pos = pos;
    }

    @Override
    public int compareTo(Orderer<T> o) {
        return pos - ((TestOrderer<T>) o).pos;
    }

    @Override
    public T element() {
        return element;
    }

}
