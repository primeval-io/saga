package io.primeval.saga.router.exception;

public interface ExceptionRecoveryProvider<T extends Throwable> extends ExceptionRecovery<T> {

    Class<T> exceptionType();
}
