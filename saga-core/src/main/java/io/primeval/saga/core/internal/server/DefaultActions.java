package io.primeval.saga.core.internal.server;

import org.osgi.util.promise.Promises;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.action.Action;
import io.primeval.saga.guava.ImmutableResult;
import io.primeval.saga.renderer.MimeTypes;

public final class DefaultActions {

    public static final Action NOT_FOUND = new Action(context -> {
        return Promises.resolved(
                ImmutableResult.notFound("Resource not found").contentType(MimeTypes.TEXT).build());
    }, TypeTag.of(String.class), new DefaultActionKey("not_found"));

}
