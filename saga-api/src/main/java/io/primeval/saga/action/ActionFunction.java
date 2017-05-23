package io.primeval.saga.action;

import java.util.function.Function;

import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

@FunctionalInterface
public interface ActionFunction extends Function<Context, Promise<Result<?>>> {

    public static ActionFunction failed(Throwable t) {
        return ctx -> Promises.failed(t);
    }
    
}
