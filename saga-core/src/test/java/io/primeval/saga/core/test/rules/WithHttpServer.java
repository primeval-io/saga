package io.primeval.saga.core.test.rules;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.primeval.codex.test.rules.WithCodex;
import io.primeval.common.test.rules.TestResource;
import io.primeval.saga.core.internal.parameter.HttpParameterConverterImpl;
import io.primeval.saga.core.internal.server.HttpServerImpl;
import io.primeval.saga.http.server.spi.HttpServerProvider;
import io.primeval.saga.http.shared.provider.ProviderProperties;
import io.primeval.saga.http.shared.provider.SagaProvider;
import io.primeval.saga.router.Router;

public class WithHttpServer extends ExternalResource implements TestResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(WithHttpServer.class);

    private HttpServerImpl httpServer;
    private WithCodex wCodex;
    private WithSerDes wSerDes;
    private Supplier<HttpServerProvider> serverProvider;
    private Supplier<Router> routerSupplier;

    private OptionalInt port;

    public WithHttpServer(WithSerDes wSerDes, WithCodex wCodex, Supplier<Router> routerSupplier, OptionalInt port,
            Supplier<HttpServerProvider> serverProvider) {
        this.wSerDes = wSerDes;
        this.wCodex = wCodex;
        this.routerSupplier = routerSupplier;
        this.port = port;
        this.serverProvider = serverProvider;
    }

    @Override
    public void before() throws Throwable {
        super.before();
        httpServer = new HttpServerImpl();
        httpServer.setSerializer(wSerDes.getSerializer());
        httpServer.setDeserializer(wSerDes.getDeserializer());
        httpServer.setParamConverter(new HttpParameterConverterImpl());

        httpServer.setDispatcher(wCodex.getDispatcher());
        httpServer.setScheduler(wCodex.getScheduler());
        httpServer.setRouter(routerSupplier.get());

        HttpServerProvider httpServerProvider = serverProvider.get();
        SagaProvider sagaProvider = httpServerProvider.getClass().getAnnotation(SagaProvider.class);
        if (sagaProvider == null) {
            LOGGER.warn("Ignoring saga provider missing @SagaProvider annotation, type {}",
                    httpServerProvider.getClass());
        } else {
            httpServer.setHttpServerProvider(httpServerProvider, new ProviderProperties(sagaProvider.name()));
        }

        httpServer.activate();

        httpServer.start(port.orElseGet(() -> findRandomOpenPortOnAllLocalInterfaces())).getValue();
    }

    @Override
    public void after() {
        try {
            httpServer.stop().getValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HttpServerImpl getHttpServer() {
        return httpServer;
    }

    public int port() {
        try {
            return httpServer.port().getValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Integer findRandomOpenPortOnAllLocalInterfaces()  {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException ie) {
            throw new RuntimeException(ie);
        }
    }

}
