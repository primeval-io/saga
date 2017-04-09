package io.primeval.saga.annotations.internal;

import java.util.Objects;

import org.osgi.framework.Bundle;

import io.primeval.saga.router.spi.RouterActionProviderKey;

public final class BundleControllerKey extends RouterActionProviderKey {

    public final Class<?> controllerClass;
    public final Bundle locationBundle;
    public final Bundle providingBundle;
    public final long serviceId;

    public BundleControllerKey(Class<?> controllerClass, Bundle locationBundle, Bundle providingBundle,
            long serviceId) {
        this.controllerClass = controllerClass;
        this.locationBundle = locationBundle;
        this.providingBundle = providingBundle;
        this.serviceId = serviceId;
    }

    @Override
    public String repr() {
        return locationBundle.getSymbolicName() + "::" + serviceId + "::" + controllerClass.getName();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = Objects.hash(controllerClass, locationBundle, providingBundle);
        result = 31 * result + (int) (serviceId ^ (serviceId >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BundleControllerKey other = (BundleControllerKey) obj;
        return serviceId == other.serviceId && Objects.equals(controllerClass, other.controllerClass)
                && Objects.equals(locationBundle, other.locationBundle)
                && Objects.equals(providingBundle, other.providingBundle);
    }

}
