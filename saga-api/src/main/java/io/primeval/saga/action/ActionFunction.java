package io.primeval.saga.action;

import java.util.function.Function;

import org.osgi.util.promise.Promise;

@FunctionalInterface
public interface ActionFunction extends Function<Context, Promise<Result<?>>> {

}
