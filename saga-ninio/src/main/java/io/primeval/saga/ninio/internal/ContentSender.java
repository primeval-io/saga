package io.primeval.saga.ninio.internal;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.davfx.ninio.core.SendCallback;
import com.davfx.ninio.http.HttpContentSender;

import io.primeval.codex.publisher.UnicastPublisher;

public final class ContentSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentSender.class);

    // Returns sizes sent.
    // Publisher completes when payload is sent.
    public static Publisher<Integer> sendPayloadAndGetAcks(HttpContentSender contentSender,
            Publisher<ByteBuffer> payload) {
        UnicastPublisher<Integer> publisher = new UnicastPublisher<>();
        payload.subscribe(new Subscriber<ByteBuffer>() {

            private Subscription sub;

            @Override
            public void onComplete() {
                try {
                    contentSender.finish();
                    publisher.complete();
                } catch (Throwable t) {
                    publisher.error(t);
                }
            }

            @Override
            public void onError(Throwable error) {
                LOGGER.error("An error happened in the flux sending the payload", error);
                contentSender.finish();
                try {
                    publisher.error(error);
                } catch (RuntimeException e) {
                    // *iif* it has already finished
                }
            }

            @Override
            public void onNext(ByteBuffer bb) {
                int remaining = bb.remaining();

                contentSender.send(bb, new SendCallback() {

                    @Override
                    public void failed(IOException error) {
                        publisher.error(error);
                        sub.cancel();
                        LOGGER.trace("Couldn't send data; potentially client just canceled.", error);

                    }

                    @Override
                    public void sent() {
                        publisher.next(remaining);
                        sub.request(1);
                    }
                });
            }

            @Override
            public void onSubscribe(Subscription sub) {
                this.sub = sub;
                sub.request(1);
            }
        });

        return publisher;
    }

    public static void sendPayload(HttpContentSender contentSender, Publisher<ByteBuffer> payload) {
        payload.subscribe(new Subscriber<ByteBuffer>() {

            private Subscription sub;

            @Override
            public void onComplete() {
                contentSender.finish();
            }

            @Override
            public void onError(Throwable error) {
                LOGGER.error("An error happened in the flux sending the payload", error);
                contentSender.finish();
            }

            @Override
            public void onNext(ByteBuffer bb) {
                contentSender.send(bb, new SendCallback() {

                    @Override
                    public void failed(IOException error) {
                        sub.cancel();
                        LOGGER.trace("Couldn't send data; potentially client just canceled.", error);

                    }

                    @Override
                    public void sent() {
                        sub.request(1);
                    }
                });
            }

            @Override
            public void onSubscribe(Subscription sub) {
                this.sub = sub;
                sub.request(1);
            }
        });

    }

}
