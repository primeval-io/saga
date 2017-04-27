package io.primeval.saga.core.internal.server.exception;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lambdacube.component.annotation.common.ServiceRanking;
import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;
import io.primeval.saga.router.Route;
import io.primeval.saga.router.exception.ExceptionRecoveryProvider;

@Component
@ServiceRanking(Integer.MIN_VALUE)
public final class ExceptionLogger implements ExceptionRecoveryProvider<Throwable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionLogger.class);

    @Override
    public Promise<Result<?>> recover(Throwable exception, Context context, Optional<Route> boundRoute) {
        LOGGER.debug("Error on route: {}", context.request().uri, exception);
        return null; // Do not change result
    }

    @Override
    public Class<Throwable> exceptionType() {
        return Throwable.class;
    }

}
