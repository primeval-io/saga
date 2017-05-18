package io.primeval.saga.core.internal.interception.action;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.google.common.collect.Maps;

import io.primeval.saga.interception.action.ActionInterceptor;
import io.primeval.saga.interception.action.ActionInterceptorManager;

@Component(immediate = true)
public final class ActionInterceptorManagerImpl implements ActionInterceptorManager {

    private final Map<Class<?>, ActionInterceptor<?>> actionInterceptors = Maps.newConcurrentMap();

    @SuppressWarnings("unchecked")
    @Override
    public <T> ActionInterceptor<T> actionInterceptor(Class<T> type) {
        return (ActionInterceptor<T>) actionInterceptors.get(type);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addActionInterceptor(ActionInterceptor<?> actionInterceptor) {
        actionInterceptors.put(actionInterceptor.type(), actionInterceptor);
    }

    public void removeActionInterceptor(ActionInterceptor<?> actionInterceptor) {
        actionInterceptors.remove(actionInterceptor.type(), actionInterceptor);
    }

}
