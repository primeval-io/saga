package io.primeval.saga.parameter;

import org.osgi.util.promise.Promise;

import io.primeval.common.type.TypeTag;

public interface HttpParameterConverter {

    <T> Promise<T> createParameter(String parameterValue, TypeTag<T> typeTag) throws HttpParameterConvertingException;

}
