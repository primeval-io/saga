package io.primeval.saga.core.internal.server;

public interface Orderer<E> extends Comparable<Orderer<E>> {

    E element();

}
