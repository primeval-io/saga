package io.primeval.saga.ninio.internal.server;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.reactivestreams.Publisher;

import com.davfx.ninio.core.Address;
import com.davfx.ninio.core.Listener;
import com.davfx.ninio.core.Ninio;
import com.davfx.ninio.core.TcpSocketServer;
import com.davfx.ninio.http.HttpListening;

import io.primeval.saga.http.server.spi.HttpServerEvent;
import io.primeval.saga.http.server.spi.HttpServerProvider;
import io.primeval.saga.http.shared.provider.SagaProvider;
import io.primeval.saga.ninio.internal.NinioSagaShared;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.UnicastProcessor;

@Component
@SagaProvider(name = NinioSagaShared.PROVIDER_NAME)
public final class NinioHttpServerProvider implements HttpServerProvider {

    private Ninio ninio;

    private Listener tcp;

    private Deferred<Void> startedDeferred;

    private Deferred<Void> closedDeferred;

    private UnicastProcessor<HttpServerEvent> emitter;

    private FluxSink<HttpServerEvent> sink;

    private int port = -1;

    @Activate
    public void activate() {
        // do nothing
    }

    @Deactivate
    public void deactivate() {
        // do nothing
    }

    @Override
    public synchronized Promise<Void> start(int port) {
        if (startedDeferred != null) {
            throw new IllegalStateException("already started");
        }

        this.port = port;

        startedDeferred = new Deferred<>();
        closedDeferred = new Deferred<>();

        emitter = UnicastProcessor.create();
        this.sink = emitter.sink();

        ninio = Ninio.create();
        tcp = ninio.create(TcpSocketServer.builder().bind(new Address(Address.ANY, port)));
        tcp.listen(ninio.create(HttpListening.builder()
                .with(new SagaHttpListeningHandler(sink, startedDeferred, closedDeferred))));

        return startedDeferred.getPromise();

    }

    @Override
    public Publisher<HttpServerEvent> eventStream() {
        return emitter;
    }

    @Override
    public synchronized Promise<Integer> port() {
        return startedDeferred.getPromise().map(x -> port);
    }

    @Override
    public synchronized Promise<Void> stop() {

        tcp.close();
        ninio.close();

        return closedDeferred.getPromise();
    }

}
