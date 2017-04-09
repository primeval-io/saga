package io.primeval.saga.core.internal.server.websocket;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Optional;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.primeval.codex.promise.PromiseHelper;
import io.primeval.codex.publisher.UnicastPublisher;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.action.Result;
import io.primeval.saga.core.internal.server.websocket.WebSocketFrame.Type;
import io.primeval.saga.http.protocol.HeaderNames;
import io.primeval.saga.http.protocol.HttpRequest;
import io.primeval.saga.http.protocol.Status;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.renderer.MimeTypes;
import io.primeval.saga.serdes.SerDes;
import io.primeval.saga.websocket.WebSocket;
import io.primeval.saga.websocket.WebSocketManager;
import io.primeval.saga.websocket.message.WebSocketMessage;
import io.primeval.saga.websocket.message.WebSocketMessage.FunVisitor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public final class WebSocketManagerImpl implements WebSocketManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketManagerImpl.class);

    private static final String WEBSOCKET_PROTOCOL = "websocket";

    private SerDes serDes;

    @Override
    public <T> Optional<WebSocket<T>> tryToCreateWebSocket(HttpRequest request, Payload incomingPayload,
            Publisher<WebSocketMessage<T>> messagePublisher, TypeTag<? extends T> typeTag) {

        WebSocket<T> webSocket = create(request, incomingPayload, messagePublisher, typeTag);
        return Optional.ofNullable(webSocket);
    }

    private <T> /* nullable */ WebSocket<T> create(HttpRequest request, Payload incomingPayload,
            Publisher<WebSocketMessage<T>> messagePublisher, TypeTag<? extends T> typeTag) {

        String wsKey = wsKey(request);

        if (wsKey == null) {
            LOGGER.error("Missing Sec-WebSocket-Key header");
            return null;
        }

        ImmutableListMultimap<String, String> responseHeaders = ImmutableListMultimap.<String, String> builder()
                .put(HeaderNames.CONNECTION, "Upgrade")
                .put(HeaderNames.UPGRADE, "websocket")
                .put("Sec-WebSocket-Accept", BaseEncoding.base64().encode(
                        Hashing.sha1()
                                .hashBytes((wsKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(Charsets.UTF_8))
                                .asBytes()))
                .build();

        // This needs to be a processor to handle backpressure ..
        UnicastPublisher<ByteBuffer> out = new UnicastPublisher<ByteBuffer>();
        UnicastPublisher<WebSocketMessage<T>> in = new UnicastPublisher<WebSocketMessage<T>>();

        Payload payload = Payload.stream(out);

        handleOutgoing(messagePublisher, in, out, typeTag);
        handleIncoming(incomingPayload.content, in, out, typeTag);

        return new WebSocket<T>() {

            @Override
            public Promise<Result<Payload>> result() {
                return Promises
                        .resolved(new Result<>(Status.SWITCHING_PROTOCOLS, Multimaps.asMap(responseHeaders), payload));
            }

            @Override
            public Publisher<WebSocketMessage<T>> incoming() {
                return in;
            }

        };

    }

    private <T> void handleOutgoing(Publisher<WebSocketMessage<T>> messagePublisher, UnicastPublisher<WebSocketMessage<T>> in,
            UnicastPublisher<ByteBuffer> out,
            TypeTag<? extends T> typeTag) {
        Flux<WebSocketMessage<T>> flux = Flux.from(messagePublisher).doOnNext(message -> {
            Publisher<ByteBuffer> publisher = message.visit(new FunVisitor<T, Publisher<ByteBuffer>>() {

                @Override
                public Publisher<ByteBuffer> visitObject(T object) {
                    Promise<Payload> payloadPms = serDes.serializer().serialize(object, typeTag, MimeTypes.JSON,
                            Collections.emptyMap());
                    return PromiseHelper.toMono(payloadPms).flatMap(payLoad -> {
                        return outFrame(payLoad, false);
                    });
                }

                @Override
                public Publisher<ByteBuffer> visitText(String text) {
                    Promise<Payload> payloadPms = serDes.serializer().serialize(text, TypeTag.of(String.class),
                            MimeTypes.TEXT,
                            Collections.singletonMap("charset", "utf-8"));
                    return PromiseHelper.toMono(payloadPms).flatMap(payLoad -> {
                        return outFrame(payLoad, true);
                    });
                }

                private Publisher<? extends ByteBuffer> outFrame(Payload payLoad, boolean text) {
                    long length = payLoad.contentLength
                            .orElseThrow(() -> new IllegalStateException(
                                    "WebSocket only accepts elements with known length"));
                    return Flux.concat(Mono.just(WebsocketUtils.headerOf(text ? 0x01 : 0x02, length)), payLoad.content);
                }
            });
            drainAndSend(out, publisher);

        }).doOnTerminate(() -> {
            out.next(WebsocketUtils.headerOf(0x08, 0)); // close frame.
            out.complete();
            in.complete();
        });

        flux.subscribe();

    }

    // Need to clean up this mess
    public static <T> void drainAndSend(UnicastPublisher<T> out, Publisher<T> payload) {
        payload.subscribe(new Subscriber<T>() {

            private Subscription sub;

            @Override
            public void onComplete() {
                // We don't close the websocket only because we sent one message...
                sub.cancel();
            }

            @Override
            public void onError(Throwable error) {
                LOGGER.error("An error happened in the flux sending the payload", error);
                try {
                    sub.cancel();
                    out.error(error);
                } catch (RuntimeException e) {
                    // *iif* it has already finished
                }
            }

            @Override
            public void onNext(T bb) {
                out.next(bb);
            }

            @Override
            public void onSubscribe(Subscription sub) {
                this.sub = sub;
                sub.request(Long.MAX_VALUE);
            }
        });
    }

    private <T> void handleIncoming(Publisher<ByteBuffer> incomingPayload, UnicastPublisher<WebSocketMessage<T>> in,
            UnicastPublisher<ByteBuffer> out, TypeTag<? extends T> typeTag) {

        WebsocketFrameReader<T> websocketFrameReader = new WebsocketFrameReader<>(out);

        Flux<WebSocketFrame> incomingFrames = Flux.from(websocketFrameReader.getIncomingFrames());

        Flux<WebSocketMessage<T>> mappedFrames = incomingFrames.flatMap(frame -> {

            if (frame.type == Type.TEXT) {
                Promise<String> pms = serDes.deserializer().deserialize(frame.payload, TypeTag.of(String.class),
                        typeTag.getClassLoader(), MimeTypes.TEXT, Collections.singletonMap("charset", "utf-8"));
                Mono<String> objMono = PromiseHelper.toMono(pms);
                return objMono.map(obj -> WebSocketMessage.text((String) obj));

            } else {
                Mono<T> objMono = PromiseHelper
                        .toMono(serDes.deserializer().deserialize(frame.payload, typeTag, typeTag.getClassLoader(),
                                MimeTypes.JSON,
                                Collections.emptyMap()));
                return objMono.map(WebSocketMessage::object);
            }

        });

        drainAndSend(in, mappedFrames);

        Flux.from(incomingPayload).doOnNext(websocketFrameReader::received).subscribe();

    }

    private static final class WebsocketFrameReader<T> {
        private boolean opcodeRead = false;
        private int currentOpcode;
        private boolean lenRead = false;
        private boolean mustReadExtendedLen16;
        private boolean mustReadExtendedLen64;
        private long currentLen;
        private long currentRead;
        private boolean mustReadMask;
        private ByteBuffer currentExtendedLenBuffer;
        private byte[] currentMask;
        private ByteBuffer currentMaskBuffer;
        private int currentPosInMask;

        private long toPing = 0L;

        private UnicastPublisher<WebSocketFrame> framesIn = new UnicastPublisher<>();
        private UnicastPublisher<ByteBuffer> out;
        private UnicastPublisher<ByteBuffer> incomingPayLoadPublisher;

        public WebsocketFrameReader(UnicastPublisher<ByteBuffer> out) {
            this.out = out;
        }

        public UnicastPublisher<WebSocketFrame> getIncomingFrames() {
            return framesIn;
        }

        public void received(ByteBuffer buffer) {

            while (buffer.hasRemaining()) {

                boolean headerReadBefore = lenRead && !mustReadExtendedLen16 && !mustReadExtendedLen64 && !mustReadMask;

                if (!opcodeRead && buffer.hasRemaining()) {
                    int v = buffer.get() & 0xFF;
                    if ((v & 0x80) != 0x80) {
                        LOGGER.error("Current implementation handles only FIN packets");
                        // sender.cancel();
                        // connection.closed();
                        return;
                    }
                    currentOpcode = v & 0x0F;
                    opcodeRead = true;
                }

                if (!lenRead && buffer.hasRemaining()) {
                    int v = buffer.get() & 0xFF;
                    int len = v & 0x7F;
                    if (len <= 125) {
                        currentLen = len;
                        mustReadExtendedLen16 = false;
                        mustReadExtendedLen64 = false;
                    } else if (len == 126) {
                        mustReadExtendedLen16 = true;
                        mustReadExtendedLen64 = false;
                        currentExtendedLenBuffer = ByteBuffer.allocate(2);
                    } else {
                        mustReadExtendedLen64 = true;
                        mustReadExtendedLen16 = false;
                        currentExtendedLenBuffer = ByteBuffer.allocate(8);
                    }
                    mustReadMask = ((v & 0x80) == 0x80);
                    if (mustReadMask) {
                        currentMask = new byte[4];
                        currentMaskBuffer = ByteBuffer.wrap(currentMask);
                        currentPosInMask = 0;
                    }
                    lenRead = true;
                }

                while (mustReadExtendedLen16 && buffer.hasRemaining()) {
                    int v = buffer.get();
                    currentExtendedLenBuffer.put((byte) v);
                    if (currentExtendedLenBuffer.position() == 2) {
                        currentExtendedLenBuffer.flip();
                        currentLen = currentExtendedLenBuffer.getShort() & 0xFFFF;
                        mustReadExtendedLen16 = false;
                        currentExtendedLenBuffer = null;
                    }
                }
                while (mustReadExtendedLen64 && buffer.hasRemaining()) {
                    int v = buffer.get();
                    currentExtendedLenBuffer.put((byte) v);
                    if (currentExtendedLenBuffer.position() == 8) {
                        currentExtendedLenBuffer.flip();
                        currentLen = currentExtendedLenBuffer.getLong();
                        mustReadExtendedLen64 = false;
                        currentExtendedLenBuffer = null;
                    }
                }
                while (mustReadMask && buffer.hasRemaining()) {
                    int v = buffer.get();
                    currentMaskBuffer.put((byte) v);
                    if (currentMaskBuffer.position() == 4) {
                        currentMaskBuffer = null;
                        mustReadMask = false;
                    }
                }

                boolean headerReadAfter = lenRead && !mustReadExtendedLen16 && !mustReadExtendedLen64 && !mustReadMask;

                boolean newFrame = !headerReadBefore && headerReadAfter;

                if (opcodeRead && lenRead && !mustReadExtendedLen16 && !mustReadExtendedLen64 && !mustReadMask) { // %%

                    ByteBuffer partialBuffer;
                    int len = (int) Math.min(buffer.remaining(), currentLen - currentRead);
                    if (currentMask == null) {
                        partialBuffer = buffer.duplicate();
                        partialBuffer.limit(partialBuffer.position() + len);
                        buffer.position(buffer.position() + len);
                        currentRead += len;
                    } else {
                        partialBuffer = ByteBuffer.allocate(len);
                        while (buffer.hasRemaining() && (currentRead < currentLen)) {
                            int v = buffer.get() & 0xFF;
                            v ^= currentMask[currentPosInMask];
                            partialBuffer.put((byte) v);
                            currentRead++;
                            currentPosInMask = (currentPosInMask + 1) % 4;
                        }
                        partialBuffer.flip();
                    }
                    int opcode = currentOpcode;
                    long frameLength = currentLen;

                    boolean frameCompleted = currentRead == currentLen;

                    if (opcode == 0x09) {
                        if (toPing == 0L) {
                            toPing = frameLength;
                            out.next(WebsocketUtils.headerOf(0x0A, frameLength));
                        }

                        toPing -= partialBuffer.remaining();
                        out.next(partialBuffer);
                    } else if (opcode == 0x01) {
                        if (newFrame) {
                            incomingPayLoadPublisher = new UnicastPublisher<>();
                            framesIn.next(WebSocketFrame.text(Payload.ofLength(currentLen, incomingPayLoadPublisher)));
                        }
                        incomingPayLoadPublisher.next(partialBuffer);

                    } else if (opcode == 0x02) {
                        if (newFrame) {
                            incomingPayLoadPublisher = new UnicastPublisher<>();
                            framesIn.next(
                                    WebSocketFrame.binary(Payload.ofLength(currentLen, incomingPayLoadPublisher)));
                        }
                        incomingPayLoadPublisher.next(partialBuffer);
                    } else if (opcode == 0x08) {
                        LOGGER.trace("Connection requested by peer");
                        framesIn.complete();
                        out.complete(); // TODO need to send something back?
                        frameCompleted = true;
                    }
                    if (frameCompleted) {
                        opcodeRead = false;
                        lenRead = false;
                        mustReadExtendedLen16 = false;
                        mustReadExtendedLen64 = false;
                        currentExtendedLenBuffer = null;
                        mustReadMask = false;
                        currentMaskBuffer = null;
                        currentMask = null;
                        currentRead = 0L;
                        if (incomingPayLoadPublisher != null) {
                            incomingPayLoadPublisher.complete();
                        }
                    }
                }
            }
        }

    }

    private String wsKey(HttpRequest request) {
        String connectionHeader = Iterables.getFirst(request.headers.get(HeaderNames.CONNECTION), null);
        if (!HeaderNames.UPGRADE.equals(connectionHeader)) {
            return null;
        }
        String upgradeHeader = Iterables.getFirst(request.headers.get(HeaderNames.UPGRADE), null);
        if (!WEBSOCKET_PROTOCOL.equals(upgradeHeader)) {
            return null;
        }
        String wsKey = Iterables.getFirst(request.headers.get("Sec-WebSocket-Key"), null);

        String wsVersion = Iterables.getFirst(request.headers.get("Sec-WebSocket-Version"), null);

        if (!"13".equals(wsVersion)) {
            LOGGER.error("Current implementation does not handle this version: " + wsVersion);
            return null;
        }

        return wsKey;
    }

    @Reference
    public void setSetDes(SerDes serDes) {
        this.serDes = serDes;
    }

}
