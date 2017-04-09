package io.primeval.saga.annotations.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.primeval.codex.promise.PromiseHelper;
import io.primeval.common.type.GenericBoxes;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.action.Action;
import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;
import io.primeval.saga.annotations.Body;
import io.primeval.saga.annotations.Path;
import io.primeval.saga.annotations.PathParameter;
import io.primeval.saga.annotations.QueryParameter;
import io.primeval.saga.http.protocol.HttpRequest;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.router.Route;
import io.primeval.saga.router.RouterAction;

@Component
public final class ControllerRouteFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerRouteFinder.class);

    private ControllerRouteProvider controllerRouteProvider;

    @Activate
    public void activate(BundleContext bundleContext) {
        controllerRouteProvider = new ControllerRouteProvider(bundleContext, this::routerActions);
        controllerRouteProvider.open();
    }

    @Deactivate
    public void deactivate() {
        controllerRouteProvider.close();
    }

    public ActionInvocationHandler createInvocationHandler(Method m, Object target,
            Function<Object, Promise<Result<?>>> wrap) {

        List<Function<Context, Promise<?>>> inject = new ArrayList<>(m.getParameterCount());
        for (Parameter parameter : m.getParameters()) {

            if (parameter.getType() == HttpRequest.class) {
                Function<Context, Promise<?>> fun = context -> {
                    return Promises.resolved(context.request());
                };
                inject.add(fun);
                continue;
            }

            QueryParameter qp = parameter.getAnnotation(QueryParameter.class);
            PathParameter pp = parameter.getAnnotation(PathParameter.class);
            Body b = parameter.getAnnotation(Body.class);

            if (qp != null) {
                // Auto-detect from param name
                String paramName = qp.value().isEmpty() ? parameter.getName() : qp.value();
                Function<Context, Promise<?>> fun = context -> {
                    return context.queryParameter(paramName, TypeTag.of(parameter.getParameterizedType()));
                };
                inject.add(fun);
            } else if (pp != null) {
                throw new UnsupportedOperationException("TODO PathParameters");
            } else if (b != null) {
                Function<Context, Promise<?>> fun = parameter.getType() == Payload.class
                        ? context -> Promises.resolved(context.body()) : context -> {
                            return context.body(TypeTag.of(parameter.getParameterizedType()));
                        };
                inject.add(fun);
            } else {
                throw new IllegalArgumentException("Method " + m + " cannot be injected");
            }
        }
        return new ActionInvocationHandler() {

            @Override
            public Promise<Result<?>> invoke(Context context) {
                return Promises
                        .all(inject.stream().map(f -> f.apply(context)).collect(Collectors.toList()))
                        .flatMap(l -> PromiseHelper.wrap(() -> m.invoke(target, l.toArray())))
                        .flatMap(res -> wrap.apply(res));
            }
        };

    }

    @SuppressWarnings("unchecked")
    public Collection<RouterAction> routerActions(Object controller) {
        Path path = controller.getClass().getAnnotation(Path.class);
        String basePath = path != null ? path.value() : "";
        Method[] methods = controller.getClass().getMethods();

        List<RouterAction> boundActions = new ArrayList<>();
        for (Method m : methods) {
            io.primeval.saga.annotations.Route routeAnn = m
                    .getAnnotation(io.primeval.saga.annotations.Route.class);

            if (routeAnn == null) {
                continue;
            }

            Route route = new io.primeval.saga.router.Route(routeAnn.method(),
                    RouteUtils.path(basePath + routeAnn.uri()));

            Function<Object, Promise<Result<?>>> wrap;
            TypeTag resultTypeTag;

            TypeTag returnTypeTag = TypeTag.of(m.getGenericReturnType());

            Class rawType = returnTypeTag.rawType();
            if (Promise.class.isAssignableFrom(rawType)) {

                TypeTag promiseTypeTag = GenericBoxes.typeParameter(returnTypeTag);

                // Promise<Result<X>> with any X
                if (Result.class.isAssignableFrom(promiseTypeTag.rawType())) {

                    resultTypeTag = GenericBoxes.typeParameter(promiseTypeTag);
                    wrap = x -> (Promise<Result<?>>) x;
                }
                // Promise<X> with X != Result
                else {
                    resultTypeTag = promiseTypeTag;
                    wrap = x -> (Promise<Result<?>>) ((Promise) x).map(Result::ok);
                }
            }
            // Result<X> with any X
            else if (rawType == Result.class) {
                resultTypeTag = GenericBoxes.typeParameter(returnTypeTag);
                wrap = x -> Promises.resolved((Result) x);

            }
            // X with X != Promise && X != Result
            else {
                resultTypeTag = returnTypeTag;
                wrap = x -> Promises.resolved(Result.ok(x));
            }

            try {
                ActionInvocationHandler actionInvocationHandler = createInvocationHandler(m, controller, wrap);
                RouterAction boundAction = new RouterAction(route,
                        new Action(actionInvocationHandler::invoke, resultTypeTag, new MethodActionKey(m)));

                boundActions.add(boundAction);
            } catch (Exception e) {
                // Maybe register it as unsatisfied.
                LOGGER.warn("Could not create action with route {} {} for method {}", route.method, route.pathPattern,
                        m.getDeclaringClass().getName() + "#" + m.getName(), e.getMessage());
                continue;
            }

        }
        return Collections.unmodifiableCollection(boundActions);
    }

}
