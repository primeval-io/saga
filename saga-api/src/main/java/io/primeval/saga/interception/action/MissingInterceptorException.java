package io.primeval.saga.interception.action;

public class MissingInterceptorException extends RuntimeException {

    public final Class<?> interceptorType;

    public MissingInterceptorException(Class<?> interceptorType) {
        super("Missing interceptor of type " + interceptorType);
        this.interceptorType = interceptorType;
    }

}
