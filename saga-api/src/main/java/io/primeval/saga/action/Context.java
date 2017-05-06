package io.primeval.saga.action;

import java.util.List;
import java.util.Optional;

import org.osgi.util.promise.Promise;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.http.protocol.HttpRequest;
import io.primeval.saga.http.shared.Payload;

public interface Context {

    HttpRequest request();

    Payload body();

    List<Optional<String>> queryParameter(String parameterName);

    // Use Optional<> if you want to make the parameter optional!
    <T> Promise<T> queryParameter(String parameterName, TypeTag<? extends T> typeTag, ClassLoader classLoader);

    default <T> Promise<T> queryParameter(String parameterName, TypeTag<? extends T> typeTag) {
        return queryParameter(parameterName, typeTag, typeTag.getClassLoader());
    }

    <T> Promise<T> body(TypeTag<? extends T> typeTag, ClassLoader classLoader);

    default <T> Promise<T> body(TypeTag<? extends T> typeTag) {
        return body(typeTag, typeTag.getClassLoader());
    }

    default <T> Promise<T> body(Class<? extends T> clazz, ClassLoader classLoader) {
        return body(TypeTag.of(clazz), classLoader);
    }

    default <T> Promise<T> body(Class<? extends T> clazz) {
        return body(clazz, clazz.getClassLoader());
    }

}
