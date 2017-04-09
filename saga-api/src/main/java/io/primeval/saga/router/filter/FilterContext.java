package io.primeval.saga.router.filter;

import org.osgi.util.promise.Promise;

import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;

public interface FilterContext {
    
    Promise<Result<?>> proceed(Context context);
    
    Context context();
    
    default Promise<Result<?>> proceed() {
        return proceed(context());
    }

}
