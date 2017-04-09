package io.primeval.saga.core.internal.http.shared;

import org.osgi.service.component.annotations.Component;
import org.osgi.util.promise.Promise;

import io.primeval.saga.http.shared.ContentTypeSelector;

@Component
public final class ContentTypeSelectorImpl implements ContentTypeSelector {

    @Override
    public Promise<String> selectContentType(String acceptedTypes) {
        return null;
    }

}
