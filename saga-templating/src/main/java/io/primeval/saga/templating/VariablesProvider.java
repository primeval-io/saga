package io.primeval.saga.templating;

import java.util.Map;

public interface VariablesProvider {
    Map<String, Object> getVariables();
}
