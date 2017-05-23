package io.primeval.saga.core.internal.server;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.promise.Promise;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.primeval.codex.dispatcher.Dispatcher;
import io.primeval.codex.scheduler.Scheduler;
import io.primeval.common.property.PropertyHelper;
import io.primeval.saga.http.server.HttpServer;
import io.primeval.saga.http.server.spi.HttpServerEvent;
import io.primeval.saga.http.server.spi.HttpServerProvider;
import io.primeval.saga.http.shared.provider.ProviderProperties;
import io.primeval.saga.interception.request.RequestInterceptor;
import io.primeval.saga.parameter.HttpParameterConverter;
import io.primeval.saga.router.Router;
import io.primeval.saga.router.exception.ExceptionRecoveryInterceptor;
import io.primeval.saga.serdes.deserializer.Deserializer;
import io.primeval.saga.serdes.serializer.Serializer;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

@Component(configurationPid = HttpServerImpl.SAGA_SERVER)
public final class HttpServerImpl implements HttpServer {

    public static final String SAGA_SERVER = "saga.server";

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerImpl.class);

    private Router router;

    private Serializer serializer;

    private Deserializer deserializer;

    private HttpParameterConverter paramConverter;

    private Dispatcher dispatcher;

    private Scheduler scheduler;

    private HttpServerProvider serverProvider;

    private ProviderProperties properties;

    private Disposable streamCancellation;

    private HttpServerEventHandler httpServerEventHandler;

    private AtomicReference<SortedMap<Orderer<RequestInterceptor>, RequestInterceptor>> routeFilterProviders = new AtomicReference<>(
            ImmutableSortedMap.of());

    private AtomicReference<ImmutableList<RequestInterceptor>> allRouteFilterProviders = new AtomicReference<>(
            ImmutableList.of());

    private AtomicReference<ImmutableSet<String>> excludeFromCompression = new AtomicReference<>(
            ImmutableSet.of());

    private ExceptionRecoveryInterceptor exceptionRecoveryInterceptor;

    @Activate
    public void activate(SagaServerConfig config) {
        httpServerEventHandler = new HttpServerEventHandler(dispatcher, router, this::currentFilterProviders,
                serializer,
                deserializer,
                paramConverter, excludeFromCompression::get);
        applyConfig(config);
    }


    @Modified
    public void updated(SagaServerConfig config) {
        applyConfig(config);
    }
    
    
    private void applyConfig(SagaServerConfig config) {
        excludeFromCompression.set(ImmutableSet.copyOf(config.excludeFromCompression()));
    }

    @Override
    public Promise<Void> start(int port) {
        LOGGER.info("Starting Saga server [backend: {}]", properties.provider);

        Promise<Void> promise = serverProvider.start(port);

        promise.onResolve(() -> {
            LOGGER.info("Saga server started [backend: {}]", properties.provider);
            Publisher<HttpServerEvent> eventStream = serverProvider.eventStream();

            streamCancellation = Flux.from(eventStream).map(HttpServerEventImpl::new)
                    .doOnNext(httpServerEventHandler::onEvent)
                    .doOnError(this::onError)
                    .doOnComplete(this::onClose)
                    .doOnCancel(this::onClose)
                    .subscribe();

        });

        return promise;

    }

    @Override
    public Promise<Integer> port() {
        return serverProvider.port();
    }

    public void onError(Throwable t) {
        LOGGER.info("OnError", t);

    }

    public void onClose() {
        LOGGER.info("OnClose");
    }

    @Override
    public Promise<Void> stop() {
        LOGGER.info("Stopping Saga HTTP server [backend: {}]", properties.provider);

        Promise<Void> promise = serverProvider.stop();
        promise.onResolve(() -> {
            LOGGER.info("Saga HTTP server stopped [backend: {}]", properties.provider);
            streamCancellation.dispose();
        });
        return promise;
    }

    public void setHttpServerProvider(HttpServerProvider serverProvider, ProviderProperties properties) {
        this.serverProvider = serverProvider;
        this.properties = properties;
    }

    @Reference(target = "(saga.provider=*)")
    public void setHttpServerProvider(HttpServerProvider serverProvider, Map<String, Object> properties) {
        String[] providerName = PropertyHelper.getProperty(ProviderProperties.PROVIDER_PROPERTY, String.class,
                properties);
        if (providerName.length != 1) {
            LOGGER.warn(
                    "Retrieved SagaServerProvider of type {} with missing or ambiguous {} property, taking first: {}",
                    serverProvider.getClass(), providerName[0]);
        }
        ProviderProperties providerProperties = new ProviderProperties(providerName[0]);
        setHttpServerProvider(serverProvider, providerProperties);
    }

    @Reference
    public void setRouter(Router router) {
        this.router = router;
    }

    @Reference
    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }

    @Reference
    public void setDeserializer(Deserializer deserializer) {
        this.deserializer = deserializer;
    }

    @Reference
    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Reference
    public void setParamConverter(HttpParameterConverter paramConverter) {
        this.paramConverter = paramConverter;
    }

    @Reference
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Collection<RequestInterceptor> currentFilterProviders() {
        return allRouteFilterProviders.get();
    }

    @Reference
    public void setExceptionRecoveryInterceptor(ExceptionRecoveryInterceptor exceptionRecoveryInterceptor) {
        this.exceptionRecoveryInterceptor = exceptionRecoveryInterceptor;
        rebuildRouteFilterProviders();
    }

    public void addRouteFilterProvider(RequestInterceptor routeFilterProvider,
            Orderer<RequestInterceptor> orderer) {
        routeFilterProviders.getAndUpdate(current -> {
            return ImmutableSortedMap
                    .<Orderer<RequestInterceptor>, RequestInterceptor> naturalOrder()
                    .put(orderer, routeFilterProvider)
                    .putAll(current)
                    .build();
        });
        rebuildRouteFilterProviders();
    }

    public void removeRouteFilterProvider(RequestInterceptor routeFilterProvider,
            Orderer<RequestInterceptor> orderer) {
        routeFilterProviders.getAndUpdate(current -> {
            return ImmutableSortedMap.copyOf(Maps.filterKeys(current, key -> key.compareTo(orderer) != 0));
        });
        rebuildRouteFilterProviders();
    }

    public void rebuildRouteFilterProviders() {
        allRouteFilterProviders.set(ImmutableList.<RequestInterceptor> builder().add(exceptionRecoveryInterceptor)
                .addAll(routeFilterProviders.get().values()).add(exceptionRecoveryInterceptor).build());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addRouteFilterProvider(RequestInterceptor routeFilterProvider,
            ServiceReference<RequestInterceptor> ref) {
        ServiceReferenceOrderer<RequestInterceptor> referenceOrderer = new ServiceReferenceOrderer<RequestInterceptor>(
                routeFilterProvider, ref);
        addRouteFilterProvider(routeFilterProvider, referenceOrderer);
    }

    public void removeRouteFilterProvider(RequestInterceptor routeFilterProvider,
            ServiceReference<RequestInterceptor> ref) {
        ServiceReferenceOrderer<RequestInterceptor> referenceOrderer = new ServiceReferenceOrderer<RequestInterceptor>(
                routeFilterProvider, ref);
        removeRouteFilterProvider(routeFilterProvider, referenceOrderer);

    }
}
