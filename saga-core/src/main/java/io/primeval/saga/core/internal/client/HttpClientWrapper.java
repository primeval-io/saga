package io.primeval.saga.core.internal.client;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import com.google.common.reflect.MutableTypeToInstanceMap;

import io.primeval.codex.context.ExecutionContextManager;
import io.primeval.codex.dispatcher.Dispatcher;
import io.primeval.codex.scheduler.Scheduler;
import io.primeval.saga.http.client.BoundHttpClient;
import io.primeval.saga.http.client.HttpClient;
import io.primeval.saga.http.client.spi.HttpClientProvider;
import io.primeval.saga.serdes.SerDes;

public final class HttpClientWrapper extends HttpClientInternal implements HttpClient {

    private final HttpClientInternal wrappee;
    private final String userAgent;
    private final boolean useExecutionContext;
    private final boolean secure;
    private final String mimeType;

    public HttpClientWrapper(HttpClientInternal wrappee, String userAgent, boolean useExecutionContext, boolean secure,
            String mimeType) {
        super();
        this.wrappee = wrappee;
        this.userAgent = userAgent;
        this.useExecutionContext = useExecutionContext;
        this.secure = secure;
        this.mimeType = mimeType;
    }

    @Override
    HttpClientProvider httpClientProvider() {
        return wrappee.httpClientProvider();
    }

    @Override
    ClassLoader exceptionClassLoader() {
        return wrappee.exceptionClassLoader();
    }

    @Override
    public HttpClient as(String userAgent) {
        if (Objects.equals(userAgent, this.userAgent)) {
            return this;
        }
        return new HttpClientWrapper(this, userAgent, this.useExecutionContext, this.secure, this.mimeType);
    }

    @Override
    public HttpClient secure(boolean secure) {
        if (secure == this.secure) {
            return this;
        }
        return new HttpClientWrapper(this, this.userAgent, this.useExecutionContext, secure, this.mimeType);
    }

    @Override
    boolean secure() {
        return secure;
    }

    @Override
    SerDes serDes() {
        return wrappee.serDes();
    }

    @Override
    void fillExecutionContextHeaders(MutableTypeToInstanceMap<Object> fullContext,
            BiConsumer<String, String> addHeader) {
        wrappee.fillExecutionContextHeaders(fullContext, addHeader);
    }

    @Override
    public HttpClient withExecutionContext() {
        if (useExecutionContext) {
            return this;
        }
        return new HttpClientWrapper(this, this.userAgent, true, this.secure, this.mimeType);
    }

    @Override
    public HttpClient withoutExecutionContext() {
        if (!useExecutionContext) {
            return this;
        }
        return new HttpClientWrapper(this, this.userAgent, false, this.secure, this.mimeType);
    }

    @Override
    public BoundHttpClient to(String host, int port) {
        return new BoundHttpClientImpl(this, host, port);
    }

    @Override
    ExecutionContextManager executionContextManager() {
        return wrappee.executionContextManager();
    }

    @Override
    boolean useExecutionContext() {
        return useExecutionContext;
    }

    @Override
    Dispatcher dispatcher() {
        return wrappee.dispatcher();
    }

    @Override
    Scheduler scheduler() {
        return wrappee.scheduler();
    }

    @Override
    Optional<String> userAgent() {
        return Optional.ofNullable(userAgent);
    }

    @Override
    public HttpClient withContentType(String mimeType) {
        if (Objects.equals(mimeType, this.mimeType)) {
            return this;
        }
        return new HttpClientWrapper(this, this.userAgent, this.useExecutionContext, this.secure, mimeType);
    }

    @Override
    Optional<String> mimeType() {
        return Optional.ofNullable(mimeType);
    }

}
