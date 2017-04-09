package io.primeval.saga.router.spi;

import java.util.Collection;

import io.primeval.saga.router.RouterAction;

public interface RouterActionProvider {

    Collection<RouterAction> routerActions();

    RouterActionProviderKey id();
}
