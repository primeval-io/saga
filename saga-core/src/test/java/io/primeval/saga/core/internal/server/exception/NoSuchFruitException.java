package io.primeval.saga.core.internal.server.exception;

import java.util.NoSuchElementException;

public class NoSuchFruitException extends NoSuchElementException {

    public final String name;

    public NoSuchFruitException(String name) {
        super("Missing fruit: " + name);
        this.name = name;
    }

}
