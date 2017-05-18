package io.primeval.saga.core.internal.server.exception;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;
import io.primeval.saga.http.protocol.Status;
import io.primeval.saga.interception.action.MissingInterceptorException;
import io.primeval.saga.router.Route;
import io.primeval.saga.router.exception.ExceptionRecoveryProvider;

@Component
public final class MissingInterceptorRecovery implements ExceptionRecoveryProvider<MissingInterceptorException> {

    @Override
    public Promise<Result<?>> recover(MissingInterceptorException exception, Context context,
            Optional<Route> boundRoute) {
        return Promises.resolved(
                Result.create(Status.SERVICE_UNAVAILABLE, "Missing interceptor: " + exception.interceptorType));
    }

    @Override
    public Class<MissingInterceptorException> exceptionType() {
        return MissingInterceptorException.class;
    }

}
