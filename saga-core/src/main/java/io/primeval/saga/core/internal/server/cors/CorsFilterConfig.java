package io.primeval.saga.core.internal.server.cors;

public @interface CorsFilterConfig {

    String[] allowed_hosts() default {};

    String[] exposed_headers();

    boolean allow_credentials() default true;

    int max_age();

}
