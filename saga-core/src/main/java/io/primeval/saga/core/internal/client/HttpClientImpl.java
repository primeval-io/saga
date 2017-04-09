package io.primeval.saga.core.internal.client;

import java.util.Optional;
import java.util.function.BiConsumer;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import com.google.common.reflect.MutableTypeToInstanceMap;

import io.primeval.codex.context.ExecutionContextManager;
import io.primeval.codex.dispatcher.Dispatcher;
import io.primeval.codex.scheduler.Scheduler;
import io.primeval.saga.http.client.BoundHttpClient;
import io.primeval.saga.http.client.HttpClient;
import io.primeval.saga.http.client.spi.HttpClientProvider;
import io.primeval.saga.serdes.SerDes;

@Component(scope = ServiceScope.BUNDLE)
public final class HttpClientImpl extends HttpClientInternal implements HttpClient {

    private static final String DEFAULT_USERAGENT = "Saga HTTPClient";
    private static final boolean DEFAULT_SECURE = false;
    private static final boolean DEFAULT_USE_EXEC_CONTEXT = true;

    private HttpClientProvider httpClientProvider;

    private Dispatcher dispatcher;
    private Scheduler scheduler;
    private ClassLoader bundleClassLoader;
    private ExecutionContextManager executionContextManager;
    private SerDes serDes;

    @Activate
    public void activate(ComponentContext componentContext) {
        Bundle usingBundle = componentContext.getUsingBundle();
        BundleWiring bundleWiring = usingBundle.adapt(BundleWiring.class);
        init(bundleWiring.getClassLoader());
    }

    public void init(ClassLoader classLoader) {
        bundleClassLoader = classLoader;
    }

    @Override
    void fillExecutionContextHeaders(MutableTypeToInstanceMap<Object> fullContext,
            BiConsumer<String, String> addHeader) {
        // if (fullContext == null) {
        // return;
        // }
        // for (Entry<TypeTag<? extends Object>, Object> entry : fullContext.entrySet()) {
        // HeaderAttributeEntry header = headerAttributeMapper.toHeader(entry.getKey(), entry.getValue())
        // .orElse(null);
        // if (header == null) {
        // continue;
        // }
        // String headerName = header.name;
        // for (String val : header.values) {
        // addHeader.accept(headerName, val);
        // }
        // }

    }

    @Override
    ClassLoader exceptionClassLoader() {
        return bundleClassLoader;
    }

    @Override
    HttpClientProvider httpClientProvider() {
        return httpClientProvider;
    }

    @Override
    Dispatcher dispatcher() {
        return dispatcher;
    }

    @Override
    Scheduler scheduler() {
        return scheduler;
    }

    @Override
    SerDes serDes() {
        return serDes;
    }
    

    @Override
    public HttpClient secure(boolean secure) {
        return new HttpClientWrapper(this, DEFAULT_USERAGENT, DEFAULT_USE_EXEC_CONTEXT, secure, null);
    }

    @Override
    boolean useExecutionContext() {
        return DEFAULT_USE_EXEC_CONTEXT;
    }

    @Override
    boolean secure() {
        return DEFAULT_SECURE;
    }

    @Override
    Optional<String> userAgent() {
        return Optional.of(DEFAULT_USERAGENT);
    }

    @Override
    public HttpClient as(String userAgent) {
        return new HttpClientWrapper(this, DEFAULT_USERAGENT, DEFAULT_USE_EXEC_CONTEXT, DEFAULT_SECURE, null);
    }

    @Override
    public HttpClient withExecutionContext() {
        return this;
    }

    @Override
    ExecutionContextManager executionContextManager() {
        return executionContextManager;
    }

    @Override
    public HttpClient withoutExecutionContext() {
        return new HttpClientWrapper(this, DEFAULT_USERAGENT, false, DEFAULT_SECURE, null);
    }

    @Override
    public BoundHttpClient to(String host, int port) {
        return new BoundHttpClientImpl(this, host, port);
    }

    @Reference
    public void setHttpClientProvider(HttpClientProvider httpClientProvider) {
        this.httpClientProvider = httpClientProvider;
    }

    @Reference
    public void setSerDes(SerDes serDes) {
        this.serDes = serDes;
    }

    @Reference
    public void setExecutionContextManager(ExecutionContextManager executionContextManager) {
        this.executionContextManager = executionContextManager;
    }

    @Reference
    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Reference
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public HttpClient withContentType(String mimeType) {
        return new HttpClientWrapper(this, DEFAULT_USERAGENT, DEFAULT_USE_EXEC_CONTEXT, DEFAULT_SECURE, mimeType);
    }

    @Override
    Optional<String> mimeType() {
        return Optional.empty();
    }

}
