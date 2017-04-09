package io.primeval.saga.core.internal.parameter;

import io.primeval.codex.promise.PromiseHelper;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.parameter.HttpParameterConverter;
import io.primeval.saga.parameter.HttpParameterConvertingException;
import org.osgi.service.component.annotations.Component;
import org.osgi.util.promise.Promise;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@Component
public final class HttpParameterConverterImpl implements HttpParameterConverter {

    @SuppressWarnings("unchecked")
    @Override
    public <T> Promise<T> createParameter(String parameterValue, TypeTag<T> typeTag) throws HttpParameterConvertingException {
        return PromiseHelper.wrap(() -> {
            Class<T> rawType = typeTag.rawType();
            if (rawType == String.class) {
                return (T) parameterValue;
            } else {
                T res = tryStaticFactory("fromString", parameterValue, rawType);
                if (res == null) {
                    res = tryStaticFactory("valueOf", parameterValue, rawType);
                }
                if (res != null) {
                    return res;
                }
            }
            throw new HttpParameterConvertingException("Can't deserialize parameter to " + typeTag);
        });
    }

    private <T> T tryStaticFactory(String factoryName, String parameterValue, Class<T> rawType) {
        try {
            Method method = rawType.getMethod(factoryName, String.class);
            if ((method.getModifiers() & Modifier.STATIC) != 0) {
                @SuppressWarnings("unchecked")
                T obj = (T) method.invoke(null, parameterValue);
                return obj;
            }
            return null;

        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            return null;
        }
    }

}
