package io.primeval.saga.http.client.method;

import io.primeval.common.type.TypeTag;

public interface HttpClientBodyMethod<T extends HttpClientBodyMethod<T>> extends HttpClientMethod<T> {

    <I> T withBody(I object, TypeTag<? extends I> typeTag);

    <I> T withBody(I object, TypeTag<? extends I> typeTag, String mimeTypeOverride);

    default <I> T withBody(I body) {
        return withBody(body, TypeTag.of(body.getClass()));
    }

    default <I> T withBody(I body, String mimeTypeOverride) {
        return withBody(body, TypeTag.of(body.getClass()), mimeTypeOverride);
    }

}
