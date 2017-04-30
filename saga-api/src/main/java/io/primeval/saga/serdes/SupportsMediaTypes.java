package io.primeval.saga.serdes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.primeval.component.annotation.properties.ComponentProperty;
import io.primeval.saga.serdes.deserializer.SerDesConstants;

@ComponentProperty(SerDesConstants.MEDIATYPE_PROPERTY)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SupportsMediaTypes {

    String[] value() default "";

}
