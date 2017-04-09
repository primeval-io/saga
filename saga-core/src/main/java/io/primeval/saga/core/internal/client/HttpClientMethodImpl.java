package io.primeval.saga.core.internal.client;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableListMultimap.Builder;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.reflect.MutableTypeToInstanceMap;

import io.primeval.codex.context.ExecutionContextSwitch;
import io.primeval.codex.promise.CancelableDeferred;
import io.primeval.codex.promise.CancelablePromise;
import io.primeval.codex.promise.DelegatingCancelablePromise;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.core.internal.ContentType;
import io.primeval.saga.core.internal.SagaCoreUtils;
import io.primeval.saga.http.client.HttpClientException;
import io.primeval.saga.http.client.HttpClientObjectResponse;
import io.primeval.saga.http.client.HttpClientRawResponse;
import io.primeval.saga.http.client.HttpClientResponse;
import io.primeval.saga.http.client.method.HttpClientMethod;
import io.primeval.saga.http.protocol.HeaderNames;
import io.primeval.saga.http.protocol.HttpMethod;
import io.primeval.saga.http.shared.Payload;
import io.primeval.common.serdes.DeserializationException;
import io.primeval.saga.serdes.deserializer.Deserializer;
import io.primeval.saga.serdes.serializer.Serializer;

public abstract class HttpClientMethodImpl<T extends HttpClientMethod<T>> implements HttpClientMethod<T> {

    private final static Logger LOGGER = LoggerFactory.getLogger(HttpClientMethodImpl.class);

    final BoundHttpClientImpl boundClient;
    final HttpMethod method;

    final String uri;
    final ListMultimap<String, String> headers = Multimaps.newListMultimap(Maps.newHashMap(), Lists::newArrayList);

    /* nullable */ Duration maxDuration;
    private int withMaxPayloadSize = -1;

    public HttpClientMethodImpl(BoundHttpClientImpl boundClient, HttpMethod method, String uri) {
        this.boundClient = boundClient;
        this.method = method;
        this.uri = uri;
    }

    @SuppressWarnings("unchecked")
    public T withHeader(String name, String value) {
        headers.put(name, value);
        return (T) this; // why?
    }

    @SuppressWarnings("unchecked")
    @Override
    public T withMaxDuration(Duration maxDuration) {
        this.maxDuration = maxDuration;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T withMaxPayloadSize(int payloadSizeInBytes) {
        this.withMaxPayloadSize = payloadSizeInBytes;
        return (T) this;
    }

    <I> Optional<Body<I>> body() {
        return Optional.empty();
    }

    @Override
    public CancelablePromise<HttpClientRawResponse> exec() {
        return exec(payload -> prepareHeaders(boundClient.httpClient, null, Stream.empty(), payload));
    }

    public CancelablePromise<HttpClientRawResponse> exec(Function<Payload, Map<String, List<String>>> headersFun) {
        HttpClientInternal httpClient = boundClient.httpClient;
        ExecutionContextSwitch executionContextSwitch = httpClient.executionContextManager().onDispatch();

        Promise<Payload> payloadPms = createPayload();

        CancelableDeferred<HttpClientRawResponse> responseDef = CancelableDeferred
                .fromPromise(payloadPms.flatMap(payload -> {
                    Map<String, List<String>> headers = headersFun.apply(payload);
                    return boundClient.httpClient.httpClientProvider().sendRequest(boundClient.destination,
                            method, uri, headers, payload);
                }));

        Promise<HttpClientRawResponse> cancelableResponsePms = responseDef.getPromise();

        Promise<HttpClientRawResponse> finalPms;
        if (maxDuration != null) {
            finalPms = httpClient.scheduler().timeLimit(cancelableResponsePms, maxDuration.toMillis(),
                    TimeUnit.MILLISECONDS,
                    "Http " + method.name() + " request to " + boundClient.destination.repr()
                            + uri + " did not complete before maximum allowed duration");
        } else {
            finalPms = cancelableResponsePms;
        }

        // Bridge the promise resolution callback back to the non-blocking dispatcher!
        Deferred<HttpClientRawResponse> deferredDispatch = new Deferred<>();
        finalPms.onResolve(() -> {
            httpClient.dispatcher().execute(() -> {
                try {
                    executionContextSwitch.apply();
                    deferredDispatch.resolveWith(finalPms);
                } finally {
                    executionContextSwitch.unapply();
                }
            }, false);
        });

        return new DelegatingCancelablePromise<HttpClientRawResponse>(
                deferredDispatch.getPromise()) {
            @Override
            public boolean cancel(String reason, boolean tryToInterrupt) {
                return responseDef.cancel(reason);
            }
        };
    }

    private Map<String, List<String>> prepareHeaders(HttpClientInternal httpClient,
            MutableTypeToInstanceMap<Object> fullContext, Stream<Map.Entry<String, String>> extraEntries,
            Payload payload) {
        Builder<String, String> headerBuilder = ImmutableListMultimap.builder();
        if (httpClient.useExecutionContext()) {
            // fill headerBuilder with executionContext
            httpClient.fillExecutionContextHeaders(fullContext, headerBuilder::put);
        }
        httpClient.mimeType().ifPresent(mt -> {
            headerBuilder.put(HeaderNames.CONTENT_TYPE, mt);
        });
        payload.contentLength
                .ifPresent(
                        contentLength -> headerBuilder.put(HeaderNames.CONTENT_LENGTH, String.valueOf(contentLength)));
        headerBuilder.putAll(headers);
        extraEntries.forEach(e -> headerBuilder.put(e));
        ImmutableListMultimap<String, String> headers = headerBuilder.build();
        return Multimaps.asMap(headers);
    }

    private Promise<Payload> createPayload() {
        Serializer serializer = boundClient.httpClient.serDes().serializer();

        return this.body().map(body -> {
            String serialMimeType = body.mimeTypeOverride != null ? body.mimeTypeOverride
                    : boundClient.httpClient.mimeType().orElse(null);
            if (serialMimeType == null) {
                LOGGER.warn("Cannot serialize payload for body: missing mimeType ({} {}:{}/{})", method.name(),
                        boundClient.host,
                        boundClient.port, uri);
                return null;
            }
            return serializer.serialize(body.value, body.type, serialMimeType, Collections.emptyMap());
        }).orElse(Promises.resolved(Payload.EMPTY));
    }

    @Override
    public <O> CancelablePromise<HttpClientObjectResponse<O>> exec(TypeTag<O> type,
            IntFunction<Throwable> codeToException) {
        HttpClientInternal httpClient = boundClient.httpClient;
        Deserializer deserializer = httpClient.serDes().deserializer();

        Promise<Set<String>> acceptTypesPms = httpClient.serDes().serDesMediaTypes(type, type.getClassLoader());

        CancelablePromise<HttpClientRawResponse> rawPromise = CancelablePromise.wrap(acceptTypesPms.flatMap(acceptTypes -> {
            return exec(
                    payload -> prepareHeaders(boundClient.httpClient, null,
                            Stream.of(Maps.immutableEntry(HeaderNames.ACCEPT, Joiner.on(',').join(acceptTypes))),
                            payload));

        }));

        Promise<HttpClientObjectResponse<O>> then = rawPromise.flatMap(resp -> {
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(httpClient.exceptionClassLoader());

                Optional<ContentType> contentType = SagaCoreUtils.determineContentType(resp.headers);
                if (!contentType.isPresent()) {
                    LOGGER.warn("Missing or ambiguous Content-Type, need to override: ({} {}:{}/{})", method.name(),
                            boundClient.host,
                            boundClient.port, uri);
                    return Promises.failed(new DeserializationException(type, "unknown"));
                }

                if (codeToException != null) {
                    Throwable t = codeToException.apply(resp.code);
                    if (t != null) {
                        return Promises.failed(t);
                    }
                }
                if (resp.code >= 400) {
                    // if (resp.payload.contentLength > 0) {
                    // try {
                    // Exception exception = httpClient.jsonMapper().mapper().readValue(resp.payLoad,
                    // Exception.class);
                    // return Promises.failed(exception);
                    // } catch (Exception e) {
                    // // Could not deserialize as JSon
                    // return Promises.failed(new HttpClientException(uri, resp.code));
                    // }
                    // } else {
                    return Promises.failed(new HttpClientException(uri, resp.code));
                    // }
                } else {

                    // Optional<MetaDeserializer> metaDeserializer = httpClient.metaDeserializer();
                    // try {
                    // metaDeserializer.ifPresent(md -> md.setTrusted(true));
                    ContentType ct = contentType.get();
                    return deserializer.deserialize(resp.payload, type, httpClient.exceptionClassLoader(), ct.mediaType,
                            ct.options)
                            .map(obj -> HttpClientResponse.object(resp.code, resp.headers, obj));
                    // } finally {
                    // metaDeserializer.ifPresent(md -> md.setTrusted(false));
                    // }
                }
            } catch (Exception e) {
                return Promises.failed(e);
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        });

        return new DelegatingCancelablePromise<HttpClientObjectResponse<O>>(
                then) {

            @Override
            public boolean cancel(String reason, boolean tryToInterrupt) {
                return rawPromise.cancel(reason, tryToInterrupt);
            }
        };

    }



}
