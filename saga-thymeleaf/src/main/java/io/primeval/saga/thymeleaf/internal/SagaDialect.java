package io.primeval.saga.thymeleaf.internal;

import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.expression.IStandardVariableExpressionEvaluator;

import io.primeval.saga.thymeleaf.internal.ongl.ClassloaderOGNLExpressionEvaluator;

public class SagaDialect extends StandardDialect {

    public SagaDialect() {
        super("SAGA", StandardDialect.PREFIX, 0);
    }

    @Override
    public IStandardVariableExpressionEvaluator getVariableExpressionEvaluator() {
        return ClassloaderOGNLExpressionEvaluator.INSTANCE;
    }
    
}
