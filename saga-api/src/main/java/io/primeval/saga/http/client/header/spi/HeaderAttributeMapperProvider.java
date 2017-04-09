package io.primeval.saga.http.client.header.spi;


import io.primeval.common.type.TypeTag;
import io.primeval.saga.http.client.header.HeaderAttributeEntry;

public interface HeaderAttributeMapperProvider<T> {

    boolean supportsHeader(String headerName);

    HeaderAttributeEntry toHeader(TypeTag<? extends T> typeTag, T value);

    Class<T> getMapperType();

}
