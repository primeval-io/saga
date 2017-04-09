package io.primeval.saga.core.internal.client;

import java.util.Optional;
import java.util.function.BiConsumer;

import com.google.common.reflect.MutableTypeToInstanceMap;

import io.primeval.codex.context.ExecutionContextManager;
import io.primeval.codex.dispatcher.Dispatcher;
import io.primeval.codex.scheduler.Scheduler;
import io.primeval.saga.http.client.HttpClient;
import io.primeval.saga.http.client.spi.HttpClientProvider;
import io.primeval.saga.serdes.SerDes;

public abstract class HttpClientInternal implements HttpClient {

    abstract ClassLoader exceptionClassLoader();

    abstract ExecutionContextManager executionContextManager();

    abstract boolean useExecutionContext();

    abstract boolean secure();

    abstract Optional<String> userAgent();

    abstract Optional<String> mimeType();

    abstract HttpClientProvider httpClientProvider();

    abstract SerDes serDes();

    abstract void fillExecutionContextHeaders(MutableTypeToInstanceMap<Object> fullContext,
            BiConsumer<String, String> addHeader);

    abstract Dispatcher dispatcher();

    abstract Scheduler scheduler();

}