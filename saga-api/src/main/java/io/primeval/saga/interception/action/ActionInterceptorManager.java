package io.primeval.saga.interception.action;

public interface ActionInterceptorManager {

    <T> ActionInterceptor<T> actionInterceptor(Class<T> type); 
    
}
