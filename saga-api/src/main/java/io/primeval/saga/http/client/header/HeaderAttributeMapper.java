package io.primeval.saga.http.client.header;

import java.util.Optional;

import io.primeval.common.type.TypeTag;

public interface HeaderAttributeMapper {

    <T> Optional<HeaderAttributeEntry> toHeader(TypeTag<? extends T> typeToken, T value);

    boolean headerAttributeExists(String headerName);

}
