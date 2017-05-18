package io.primeval.saga.templating.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.primeval.saga.annotations.intercept.InterceptAction;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@InterceptAction
public @interface View {

    String name();

    // empty engine = guess from Manifest
    String engine() default "";

}
