package io.primeval.saga.http.shared;

import org.osgi.util.promise.Promise;

public interface ContentTypeSelector {
    
    Promise<String> selectContentType(String acceptedTypes);

}
