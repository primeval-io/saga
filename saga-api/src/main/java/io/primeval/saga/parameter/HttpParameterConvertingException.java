package io.primeval.saga.parameter;

public final class HttpParameterConvertingException extends RuntimeException {

    public HttpParameterConvertingException(String message) {
        super(message);
    }

    public HttpParameterConvertingException(Throwable cause) {
        super(cause);
    }

}
