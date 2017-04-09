package io.primeval.saga.router;

import java.util.Objects;

import io.primeval.saga.action.Action;

public final class RouterAction {

    public final Route route;
    public final Action action;

    public RouterAction(Route route, Action action) {
        this.route = route;
        this.action = action;
    }

    @Override
    public int hashCode() {
        return Objects.hash(route);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RouterAction other = (RouterAction) obj;

        return Objects.equals(route, other.route);
    }

}
