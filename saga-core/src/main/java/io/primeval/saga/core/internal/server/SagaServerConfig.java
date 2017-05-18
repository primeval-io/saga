package io.primeval.saga.core.internal.server;

import io.primeval.saga.renderer.MimeTypes;

public @interface SagaServerConfig {

    String[] excludeFromCompression() default { MimeTypes.BINARY };

}
