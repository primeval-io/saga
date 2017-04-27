package io.primeval.saga.examples.helloworld;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;
import io.primeval.saga.guava.ImmutableResult;
import io.primeval.saga.http.protocol.Status;
import io.primeval.saga.renderer.MimeTypes;
import io.primeval.saga.router.Route;
import io.primeval.saga.router.exception.ExceptionRecoveryProvider;

@Component
public final class NoSuchElementExceptionMapper
        implements ExceptionRecoveryProvider<NoSuchElementException> {

    @Override
    public Class<NoSuchElementException> exceptionType() {
        return NoSuchElementException.class;
    }

    @Override
    public Promise<Result<?>> recover(NoSuchElementException exception, Context context, Optional<Route> boundRoute) {
        return Promises.resolved(ImmutableResult.builder(exception.getMessage()).withStatusCode(Status.NOT_FOUND)
                .contentType(MimeTypes.TEXT).build());
    }

}
