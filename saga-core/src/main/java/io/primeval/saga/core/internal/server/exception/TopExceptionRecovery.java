package io.primeval.saga.core.internal.server.exception;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

import io.primeval.component.annotation.properties.common.ServiceRanking;
import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;
import io.primeval.saga.guava.ImmutableResult;
import io.primeval.saga.http.protocol.Status;
import io.primeval.saga.router.Route;
import io.primeval.saga.router.exception.ExceptionRecoveryProvider;
import io.primeval.saga.router.exception.ExceptionalResult;

@Component
@ServiceRanking(Integer.MAX_VALUE)
public final class TopExceptionRecovery implements ExceptionRecoveryProvider<Throwable> {

    @Override
    public Promise<Result<?>> recover(Throwable exception, Context context, Optional<Route> boundRoute) {
        return Promises.resolved(ImmutableResult.builder(new ExceptionalResult(context.request(), exception))
                .withStatusCode(Status.INTERNAL_SERVER_ERROR).build());
    }

    @Override
    public Class<Throwable> exceptionType() {
        return Throwable.class;
    }

}
