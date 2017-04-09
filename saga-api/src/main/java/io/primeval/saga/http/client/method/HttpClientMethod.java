package io.primeval.saga.http.client.method;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.IntFunction;

import org.osgi.util.promise.Promise;

import io.primeval.codex.promise.CancelablePromise;
import io.primeval.codex.promise.DelegatingCancelablePromise;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.http.client.HttpClientObjectResponse;
import io.primeval.saga.http.client.HttpClientRawResponse;

public interface HttpClientMethod<T extends HttpClientMethod<T>> {

    T withHeader(String name, String value);

    default T withMaxDuration(long delay, ChronoUnit unit) {
        return withMaxDuration(Duration.of(delay, unit));
    }

    T withMaxDuration(Duration duration);
    
    T withMaxPayloadSize(int bufferSizeInBytes);

    CancelablePromise<HttpClientRawResponse> exec();

    <O> CancelablePromise<HttpClientObjectResponse<O>> exec(TypeTag<O> type, IntFunction<Throwable> codeToException);

    default <O> CancelablePromise<HttpClientObjectResponse<O>> exec(TypeTag<O> type) {
        return exec(type, null);
    }

    default <O> CancelablePromise<HttpClientObjectResponse<O>> exec(Class<O> type, IntFunction<Throwable> codeToException) {
        return exec(TypeTag.of(type), codeToException);
    }

    default <O> CancelablePromise<HttpClientObjectResponse<O>> exec(Class<O> type) {
        return exec(type, null);
    }

    default <O> CancelablePromise<O> execMap(TypeTag<O> type, IntFunction<Throwable> codeToException) {
        CancelablePromise<HttpClientObjectResponse<O>> cancelablePromise = exec(type, codeToException);
        Promise<O> mappedPromise = cancelablePromise.map(r -> r.object);
        return new DelegatingCancelablePromise<O>(mappedPromise) {
            @Override
            public boolean cancel(String reason, boolean tryToInterrupt) {
                return cancelablePromise.cancel(reason, tryToInterrupt);
            }
        };
    }

    default <O> CancelablePromise<O> execMap(TypeTag<O> type) {
        return execMap(type, null);
    }

    default <O> CancelablePromise<O> execMap(Class<O> type, IntFunction<Throwable> codeToException) {
        return execMap(TypeTag.of(type), codeToException);
    }

    default <O> CancelablePromise<O> execMap(Class<O> type) {
        return execMap(type, null);
    }

}
