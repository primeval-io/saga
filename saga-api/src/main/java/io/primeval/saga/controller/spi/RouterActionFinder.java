package io.primeval.saga.controller.spi;

import java.util.Collection;

import io.primeval.saga.router.RouterAction;

public interface RouterActionFinder {

    Collection<RouterAction> routerActions(Object controller);

}
