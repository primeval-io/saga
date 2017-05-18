package io.primeval.saga.templating.internal;

import java.util.Map;

import io.primeval.reflex.Reflex;
import io.primeval.saga.templating.VariablesProvider;

public final class ObjectVariablesProvider implements VariablesProvider {

    public final Object source;
    private volatile Map<String, Object> map;

    public ObjectVariablesProvider(Object source) {
        this.source = source;
    }

    @Override
    public Map<String, Object> getVariables() {
        if (map == null) {
            map = Reflex.asMap(source);
        }
        return map;
    }

}
