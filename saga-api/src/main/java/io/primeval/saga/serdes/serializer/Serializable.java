package io.primeval.saga.serdes.serializer;

import java.util.Optional;

import io.primeval.common.type.TypeTag;

public interface Serializable<T> {

    T value();

    Optional<TypeTag<? extends T>> explicitTypeTag();

    default TypeTag<? extends T> typeTag() {
        return explicitTypeTag().orElseGet(() -> {
            @SuppressWarnings("unchecked")
            Class<? extends T> clazz = (Class<? extends T>) value().getClass();
            return TypeTag.of(clazz);
        });
    }

    public static <T> Serializable<T> of(T object) {
        return new SerializableImpl<>(object, null);
    }

    public static <T> Serializable<T> of(T object, TypeTag<? extends T> typeTag) {
        if (!typeTag.rawType().isAssignableFrom(object.getClass())) {
            throw new IllegalArgumentException("Object type (" + object.getClass().getName()
                    + ") is not compatible with TypeTag (" + typeTag.toString() + ")");
        }
        return new SerializableImpl<>(object, typeTag);
    }

}
