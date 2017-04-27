package io.primeval.saga.core.internal.server;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.promise.Promise;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;

import io.primeval.codex.dispatcher.Dispatcher;
import io.primeval.codex.scheduler.Scheduler;
import io.primeval.common.property.PropertyHelper;
import io.primeval.saga.core.internal.server.exception.ExceptionMappingFilterProvider;
import io.primeval.saga.http.server.HttpServer;
import io.primeval.saga.http.server.spi.HttpServerEvent;
import io.primeval.saga.http.server.spi.HttpServerProvider;
import io.primeval.saga.http.shared.provider.ProviderProperties;
import io.primeval.saga.parameter.HttpParameterConverter;
import io.primeval.saga.router.Router;
import io.primeval.saga.router.filter.RouteFilterProvider;
import io.primeval.saga.serdes.deserializer.Deserializer;
import io.primeval.saga.serdes.serializer.Serializer;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

@Component
public final class HttpServerImpl implements HttpServer {

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

    private AtomicReference<SortedMap<Orderer<RouteFilterProvider>, RouteFilterProvider>> routeFilterProviders = new AtomicReference<>(
            ImmutableSortedMap.of());

    private AtomicReference<ImmutableList<RouteFilterProvider>> allRouteFilterProviders = new AtomicReference<>(
            ImmutableList.of());

    private ExceptionMappingFilterProvider exceptionMappingFilterProvider;

    @Activate
    public void activate() {
        httpServerEventHandler = new HttpServerEventHandler(dispatcher, router, this::currentFilterProviders,
                serializer,
                deserializer,
                paramConverter);
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

    public Collection<RouteFilterProvider> currentFilterProviders() {
        return allRouteFilterProviders.get();
    }

    @Reference
    public void setExceptionMappingFilterProvider(ExceptionMappingFilterProvider exceptionMappingFilterProvider) {
        this.exceptionMappingFilterProvider = exceptionMappingFilterProvider;
        rebuildRouteFilterProviders();;
    }

    public void addRouteFilterProvider(RouteFilterProvider routeFilterProvider,
            Orderer<RouteFilterProvider> orderer) {
        routeFilterProviders.getAndUpdate(current -> {
            return ImmutableSortedMap
                    .<Orderer<RouteFilterProvider>, RouteFilterProvider> naturalOrder()
                    .put(orderer, routeFilterProvider)
                    .putAll(current)
                    .build();
        });
        rebuildRouteFilterProviders();
    }

    public void removeRouteFilterProvider(RouteFilterProvider routeFilterProvider,
            Orderer<RouteFilterProvider> orderer) {
        routeFilterProviders.getAndUpdate(current -> {
            return ImmutableSortedMap.copyOf(Maps.filterKeys(current, key -> key.compareTo(orderer) != 0));
        });
        rebuildRouteFilterProviders();
    }

    public void rebuildRouteFilterProviders() {
        allRouteFilterProviders.set(ImmutableList.<RouteFilterProvider> builder().add(exceptionMappingFilterProvider)
                .addAll(routeFilterProviders.get().values()).build());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addRouteFilterProvider(RouteFilterProvider routeFilterProvider,
            ServiceReference<RouteFilterProvider> ref) {
        ServiceReferenceOrderer<RouteFilterProvider> referenceOrderer = new ServiceReferenceOrderer<RouteFilterProvider>(
                routeFilterProvider, ref);
        addRouteFilterProvider(routeFilterProvider, referenceOrderer);
    }

    public void removeRouteFilterProvider(RouteFilterProvider routeFilterProvider,
            ServiceReference<RouteFilterProvider> ref) {
        ServiceReferenceOrderer<RouteFilterProvider> referenceOrderer = new ServiceReferenceOrderer<RouteFilterProvider>(
                routeFilterProvider, ref);
        removeRouteFilterProvider(routeFilterProvider, referenceOrderer);

    }
}
