package io.primeval.saga.ninio.internal.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.reactivestreams.Publisher;

import com.davfx.ninio.http.HttpContentReceiver;
import com.davfx.ninio.http.HttpHeaderKey;
import com.davfx.ninio.http.HttpReceiver;
import com.davfx.ninio.http.HttpResponse;
import com.davfx.ninio.http.util.HttpClient;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.primitives.Longs;

import io.primeval.saga.http.client.HttpClientRawResponse;
import io.primeval.saga.http.client.HttpClientResponse;
import io.primeval.saga.http.client.spi.HttpClientProvider;
import io.primeval.saga.http.protocol.HttpHost;
import io.primeval.saga.http.protocol.HttpMethod;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.http.shared.provider.SagaProvider;
import io.primeval.saga.ninio.internal.ContentReceiver;
import io.primeval.saga.ninio.internal.ContentSender;
import io.primeval.saga.ninio.internal.NinioSagaShared;

@Component
@SagaProvider(name = NinioSagaShared.PROVIDER_NAME)
public final class NinioHttpClientProvider implements HttpClientProvider {
    com.davfx.ninio.http.util.HttpClient httpClient;

    @Activate
    public void activate() {
        httpClient = new HttpClient();
    }

    @Deactivate
    public void deactivate() {
        httpClient.close();
    }

    @Override
    public Promise<HttpClientRawResponse> sendRequest(HttpHost destination, HttpMethod method, String uri,
            Map<String, List<String>> headers, Payload payload) {
        try {
            Deferred<HttpClientRawResponse> deferred = new Deferred<>();

            com.davfx.ninio.http.HttpContentSender contentSender = httpClient.request().host(destination.host)
                    .port(destination.port).path(uri).secure(destination.protocol.endsWith("s"))
                    .headers(NinioSagaShared.fromSagaHeaders(headers, payload))
                    .receive(new HttpReceiver() {
                        @Override
                        public HttpContentReceiver received(HttpResponse httpResponse) {
                            ImmutableListMultimap<String, String> r = (ImmutableListMultimap<String, String>) httpResponse.headers;
                            ContentReceiver contentReceiver = new ContentReceiver();
                            Publisher<ByteBuffer> publisher = contentReceiver.asPublisher();
                            ImmutableList<String> contentLength = r.get(HttpHeaderKey.CONTENT_LENGTH);
                            Long longLength = null;
                            if (r.size() == 1) {
                                String length = contentLength.get(0);
                                longLength = Longs.tryParse(length);
                            }
                            Payload payload = longLength != null ? Payload.ofLength(longLength, publisher) : Payload.stream(publisher);

                            HttpClientRawResponse rawResponse = HttpClientResponse.raw(httpResponse.status, Multimaps.asMap(r), payload);
                            deferred.resolve(rawResponse);
                            return contentReceiver;
                        }

                        @Override
                        public void failed(IOException error) {
                            deferred.fail(error);
                        }
                    }).send(NinioSagaShared.fromSagaMethod(method));

            ContentSender.sendPayload(contentSender, payload.content);

            return deferred.getPromise();

        } catch (Exception e) {
            return Promises.failed(e);
        }
    }

}
