package io.primeval.saga.annotations.internal;

import static java.text.MessageFormat.format;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import io.primeval.saga.SagaConstants;
import io.primeval.saga.router.RouterAction;
import io.primeval.saga.router.spi.RouterActionProvider;
import io.primeval.saga.router.spi.RouterActionProviderKey;

public final class ControllerRouteProvider {

    private static final class ControllerRouterActionProvider implements RouterActionProvider {

        private final RouterActionProviderKey key;
        private final Collection<RouterAction> routerActions;

        public ControllerRouterActionProvider(RouterActionProviderKey key, Collection<RouterAction> routerActions) {
            this.key = key;
            this.routerActions = routerActions;
        }

        @Override
        public Collection<RouterAction> routerActions() {
            return routerActions;
        }

        @Override
        public RouterActionProviderKey id() {
            return key;
        }
    }

    private final ServiceTracker<Object, Object> serviceTracker;

    private final Map<RouterActionProviderKey, ServiceRegistration<RouterActionProvider>> registeredProviders = new ConcurrentHashMap<>();

    public ControllerRouteProvider(BundleContext bundleContext, Function<Object, Collection<RouterAction>> routerActionsFun) {
        try {
            serviceTracker = new ServiceTracker<Object, Object>(bundleContext,
                    FrameworkUtil.createFilter(format("({0}=*)", SagaConstants.SAGA_CONTROLLER)),
                    new ServiceTrackerCustomizer<Object, Object>() {

                        @Override
                        public Object addingService(ServiceReference<Object> reference) {

                            Object service = bundleContext.getService(reference);

                            Collection<RouterAction> routerActions = routerActionsFun.apply(service);

                            if (routerActions.isEmpty()) {
                                return null;
                            }

                            BundleControllerKey key = makeKey(reference, service);

                            RouterActionProvider actionProvider = new ControllerRouterActionProvider(key, routerActions);

                            ServiceRegistration<RouterActionProvider> registration = bundleContext
                                    .registerService(RouterActionProvider.class, actionProvider, null);

                            registeredProviders.put(key, registration);
                            return service;
                        }

                        @Override
                        public void modifiedService(ServiceReference<Object> reference, Object service) {
                            // do nothing, classes can't change.
                        }

                        @Override
                        public void removedService(ServiceReference<Object> reference, Object service) {
                            BundleControllerKey key = makeKey(reference, service);

                            ServiceRegistration<RouterActionProvider> reg = registeredProviders.remove(key);
                            if (reg != null) {
                                try {
                                    reg.unregister();
                                } catch (IllegalStateException ise) {
                                    // ignore;
                                }
                            }
                        }

                    });
        } catch (InvalidSyntaxException e) {
            throw new AssertionError(e); // can never happen
        }
    }

    public void open() {
        serviceTracker.open();
    }

    public void close() {
        serviceTracker.close();
    }

    private BundleControllerKey makeKey(ServiceReference<Object> reference, Object service) {
        Class<? extends Object> controllerClass = service.getClass();
        BundleControllerKey key = new BundleControllerKey(controllerClass,
                FrameworkUtil.getBundle(controllerClass), reference.getBundle(),
                (long) reference.getProperty(Constants.SERVICE_ID));
        return key;
    }

}
