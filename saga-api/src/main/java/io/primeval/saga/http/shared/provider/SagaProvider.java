package io.primeval.saga.http.shared.provider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.lambdacube.component.annotation.ComponentProperty;
import io.lambdacube.component.annotation.ComponentPropertyGroup;

@ComponentPropertyGroup
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SagaProvider {

    @ComponentProperty(ProviderProperties.PROVIDER_PROPERTY)
    String name();

}
