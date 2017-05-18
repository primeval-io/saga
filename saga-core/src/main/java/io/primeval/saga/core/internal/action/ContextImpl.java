package io.primeval.saga.core.internal.action;

import java.util.List;
import java.util.Optional;

import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

import com.google.common.collect.ImmutableList;

import io.primeval.codex.promise.PromiseHelper;
import io.primeval.common.serdes.DeserializationException;
import io.primeval.common.type.GenericBoxes;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.action.Context;
import io.primeval.saga.core.internal.ContentType;
import io.primeval.saga.core.internal.SagaCoreUtils;
import io.primeval.saga.http.protocol.HttpRequest;
import io.primeval.saga.http.server.spi.HttpServerEvent;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.parameter.HttpParameterConverter;
import io.primeval.saga.parameter.HttpParameterConvertingException;
import io.primeval.saga.serdes.deserializer.Deserializer;

public final class ContextImpl implements Context {

    private final HttpServerEvent event;
    private final Deserializer deserializer;
    private final HttpParameterConverter paramConverter;

    public ContextImpl(HttpServerEvent event, Deserializer deserializer, HttpParameterConverter paramConverter) {
        this.event = event;
        this.deserializer = deserializer;
        this.paramConverter = paramConverter;
    }

    @Override
    public List<Optional<String>> queryParameter(String parameterName) {
        List<Optional<String>> list = event.request().parameters.get(parameterName);
        if (list != null) {
            return list;
        } else {
            return ImmutableList.of();
        }
    }

    @Override
    public HttpRequest request() {
        return event.request();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Promise<T> queryParameter(String parameterName, TypeTag<? extends T> typeTag, ClassLoader classLoader) {
        List<Optional<String>> queryParameter = queryParameter(parameterName);
        if (List.class.isAssignableFrom(typeTag.rawType())) {
            TypeTag<Object> wantedType = GenericBoxes.typeParameter(typeTag);
            ImmutableList.Builder builder = ImmutableList.builder();
            if (queryParameter != null) {
                for (Optional<String> queryParameterValue : queryParameter) {
                    queryParameterValue.ifPresent(s -> builder.add(paramConverter.createParameter(s, wantedType)));
                }
            }
            return PromiseHelper.allSuccessful(builder.build());
        } else {
            boolean required = !Optional.class.isAssignableFrom(typeTag.rawType());
            if (queryParameter == null || queryParameter.isEmpty()) {
                if (required) {
                    return Promises.failed(new HttpParameterConvertingException("missing parameter: " + parameterName));
                } else {
                    return Promises.resolved((T) Optional.empty());
                }
            } else if (queryParameter.size() > 0) {

                TypeTag wantedType = required ? typeTag : GenericBoxes.typeParameter(typeTag);
                // For now take only the first param
                Optional<String> paramValue = queryParameter.get(0);
                if (paramValue.isPresent()) {
                    Promise<Object> parameter = paramConverter.createParameter(paramValue.get(), wantedType);
                    if (required) {
                        return (Promise<T>) parameter;
                    } else {
                        return (Promise<T>) parameter.map(Optional::of);
                    }
                } else {
                    if (required) {
                        return Promises
                                .failed(new HttpParameterConvertingException("missing parameter: " + parameterName));
                    } else {
                        return Promises.resolved((T) Optional.empty());
                    }
                }
            }
            if (required) {
                return Promises.failed(new HttpParameterConvertingException("missing parameter: " + parameterName));
            } else {
                return Promises.resolved((T) Optional.empty());
            }
        }
    }

    @Override
    public Payload body() {
        return event.content();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Promise<T> body(TypeTag<? extends T> typeTag, ClassLoader classLoader) {
        Optional<ContentType> contentType = SagaCoreUtils.determineContentType(event.request().headers);
        if (contentType.isPresent()) {
            ContentType ct = contentType.get();
            return (Promise<T>) deserializer.deserialize(body(), typeTag, classLoader, ct.mediaType, ct.options);
        }
        return Promises.failed(new DeserializationException(typeTag, "unknown"));
    }

}
