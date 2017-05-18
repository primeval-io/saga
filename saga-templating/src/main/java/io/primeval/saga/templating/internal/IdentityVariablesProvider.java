package io.primeval.saga.templating.internal;

import java.util.Map;

import io.primeval.saga.templating.VariablesProvider;

public final class IdentityVariablesProvider implements VariablesProvider {

    public final Map<String, Object> map;

    public IdentityVariablesProvider(Map<String, Object> map) {
        this.map = map;
    }
    
    @Override
    public Map<String, Object> getVariables() {
        return map;
    }

}
