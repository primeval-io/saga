package io.primeval.saga.serdes.serializer;

import java.util.Optional;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.http.shared.Payload;

public interface Serializable<T> {

    public static final Serializable<Payload> EMPTY_PAYLOAD = Serializable.of(Payload.EMPTY, Payload.TYPETAG);

    T value();

    Optional<TypeTag<? extends T>> explicitTypeTag();

    default TypeTag<? extends T> typeTag() {
        return explicitTypeTag().orElseGet(() -> {
            T value = value();
            Class<?> clazz = value != null ? value.getClass() : Object.class;
            @SuppressWarnings("unchecked")
            TypeTag<? extends T> tt = (TypeTag<? extends T>) TypeTag.of(clazz);
            return tt;
        });
    }

    public static <T> Serializable<T> of(T object) {
        return new SerializableImpl<>(object, null);
    }

    public static <T> Serializable<T> of(T object, TypeTag<? extends T> typeTag) {
        if (object != null && !typeTag.rawType().isAssignableFrom(object.getClass())) {
            throw new IllegalArgumentException("Object type (" + object.getClass().getName()
                    + ") is not compatible with TypeTag (" + typeTag.toString() + ")");
        }
        return new SerializableImpl<>(object, typeTag);
    }

}
