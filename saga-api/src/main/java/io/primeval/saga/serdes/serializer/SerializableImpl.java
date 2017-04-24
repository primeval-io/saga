package io.primeval.saga.serdes.serializer;

import java.util.Objects;
import java.util.Optional;

import io.primeval.common.type.TypeTag;

public final class SerializableImpl<T> implements Serializable<T> {

    public final T object;
    public final TypeTag<? extends T> typeTag;

    SerializableImpl(T object, TypeTag<? extends T> typeTag) {
        this.object = object;
        this.typeTag = typeTag;
    }

    @Override
    public T value() {
        return object;
    }

    @Override
    public Optional<TypeTag<? extends T>> explicitTypeTag() {
        return Optional.ofNullable(typeTag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(object, typeTag);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SerializableImpl<?> other = (SerializableImpl<?>) obj;
        return Objects.equals(object, other.object) && Objects.equals(typeTag, other.typeTag);
    }

}
