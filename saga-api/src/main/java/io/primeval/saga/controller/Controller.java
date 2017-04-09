package io.primeval.saga.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.lambdacube.component.annotation.ComponentProperty;
import io.lambdacube.component.annotation.EnsureProvideService;
import io.primeval.saga.SagaConstants;

/**
 * Declares a controller.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ComponentProperty(SagaConstants.SAGA_CONTROLLER)
@EnsureProvideService
public @interface Controller {
}
