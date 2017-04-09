package io.primeval.saga.core.internal.client;

import io.primeval.common.type.TypeTag;

public final class Body<I> {
    public final TypeTag<? extends I> type;
    public final I value;
    public final String mimeTypeOverride;

    public Body(I value, TypeTag<? extends I> type, String mimeTypeOverride) {
        this.value = value;
        this.type = type;
        this.mimeTypeOverride = mimeTypeOverride;
    }

}
