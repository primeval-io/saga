package io.primeval.saga.ninio.internal.server;

import com.davfx.ninio.core.Address;
import com.davfx.ninio.http.HttpContentReceiver;
import com.davfx.ninio.http.HttpContentSender;
import com.davfx.ninio.http.HttpHeaderKey;
import com.davfx.ninio.http.HttpListeningHandler;
import com.davfx.ninio.http.HttpMessage;
import com.davfx.ninio.http.HttpRequest;
import com.davfx.ninio.http.HttpResponse;
import com.davfx.ninio.http.HttpStatus;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Longs;
import io.primeval.saga.http.server.spi.HttpServerEvent;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.ninio.internal.ContentReceiver;
import io.primeval.saga.ninio.internal.ContentSender;
import io.primeval.saga.ninio.internal.NinioSagaShared;
import org.osgi.util.promise.Deferred;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.BlockingSink;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class SagaHttpListeningHandler implements HttpListeningHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SagaHttpListeningHandler.class);

    private final BlockingSink<HttpServerEvent> eventSink;

    private final Deferred<Void> startedDeferred;
    private final Deferred<Void> closedDeferred;

    public SagaHttpListeningHandler(BlockingSink<HttpServerEvent> sink, Deferred<Void> startedDeferred,
                                    Deferred<Void> closedDeferred) {
        this.eventSink = sink;
        this.startedDeferred = startedDeferred;
        this.closedDeferred = closedDeferred;
    }

    @Override
    public void failed(IOException e) {
        System.out.println("failed");
        e.printStackTrace();
        try {
            startedDeferred.fail(e);
        } catch (IllegalStateException ignored) {
            // TODO
            System.out.println("ignored");
        }
    }

    @Override
    public void closed() {
        this.eventSink.complete();
        closedDeferred.resolve(null);
    }

    @Override
    public void connected(Address address) {
        startedDeferred.resolve(null);
    }

    @Override
    public HttpContentReceiver handle(HttpRequest request, HttpResponseSender responseSender) {
        ContentReceiver contentReceiver = new ContentReceiver();
        Publisher<ByteBuffer> inFlux = contentReceiver.asPublisher();

        Payload payload = makePayload(request, responseSender, inFlux);
        if (payload == null) {
            responseSender.send(new HttpResponse(HttpStatus.BAD_REQUEST, HttpMessage.BAD_REQUEST)).finish();
            return IgnoreContentHttpContentReceiver.INSTANCE;
        }

        HttpServerEvent incomingHttpRequest = new HttpServerEvent() {
            io.primeval.saga.http.protocol.HttpRequest sagaRequest = NinioSagaShared.toSagaRequest(request);

            @Override
            public void respond(io.primeval.saga.http.protocol.HttpResponse response, Payload payload) {
                HttpResponse ninioResponse = NinioSagaShared.fromSagaResponse(response, payload);
                HttpContentSender contentSender = responseSender.send(ninioResponse);

                ContentSender.sendPayload(contentSender, payload.content);
            }

            @Override
            public Payload content() {
                return payload;
            }

            @Override
            public io.primeval.saga.http.protocol.HttpRequest request() {
                return sagaRequest;
            }
        };
        eventSink.accept(incomingHttpRequest);

        return contentReceiver;
    }

    private Payload makePayload(HttpRequest request, HttpResponseSender responseSender, Publisher<ByteBuffer> inFlux) {
        Payload payload;
        ImmutableList<String> contentLengthHeaders = request.headers.get(HttpHeaderKey.CONTENT_LENGTH).asList();
        if (contentLengthHeaders.isEmpty()) {
            payload = Payload.stream(inFlux);
        } else {
            Long contentLength = Longs.tryParse(contentLengthHeaders.get(0));
            if (contentLength == null || contentLengthHeaders.size() > 1) {
                LOGGER.trace("Bad request with ambiguous Content-Length header, ignoring it");
                return null;
            }
            payload = Payload.ofLength(contentLength, inFlux);
        }
        return payload;
    }

}
