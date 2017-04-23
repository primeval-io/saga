package io.primeval.saga.action;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.primeval.common.type.TypeTag;

/* package*/ final class SimpleResult<T> implements Result<T> {

    public final int statusCode;
    public final T contents;
    public final Optional<TypeTag<T>> explicitType;

    public SimpleResult(int statusCode, T contents, Optional<TypeTag<T>> explicitType) {
        this.statusCode = statusCode;
        this.contents = contents;
        this.explicitType = explicitType;
    }

    @Override
    public int statusCode() {
        return statusCode;
    }

    @Override
    public Map<String, List<String>> headers() {
        return Collections.emptyMap();
    }

    @Override
    public T contents() {
        return contents;
    }

    @Override
    public Optional<TypeTag<T>> explicitType() {
        return explicitType;
    }

}
