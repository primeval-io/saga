package io.primeval.saga.annotations.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.util.promise.Promise;

import io.primeval.saga.action.ActionFunction;
import io.primeval.saga.action.ActionKey;
import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;
import io.primeval.saga.annotations.ext.ContentType;
import io.primeval.saga.guava.ImmutableResult;
import io.primeval.saga.interception.action.ActionInterceptor;

@Component
public final class ContentTypeActionInterceptor implements ActionInterceptor<ContentType> {

    @Override
    public Promise<Result<?>> onAction(ContentType contentType, Context context, ActionKey key,
            ActionFunction actionFunction) {
        return actionFunction.apply(context)
                .map(r -> ImmutableResult.copySetupAndContentOf(r).contentType(contentType.value()).build());
    }

    @Override
    public Class<ContentType> type() {
        return ContentType.class;
    }

}
