package io.primeval.saga.annotations.internal;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.primeval.codex.promise.PromiseHelper;
import io.primeval.common.type.GenericBoxes;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.action.Action;
import io.primeval.saga.action.ActionFunction;
import io.primeval.saga.action.ActionKey;
import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;
import io.primeval.saga.annotations.Body;
import io.primeval.saga.annotations.Path;
import io.primeval.saga.annotations.PathParameter;
import io.primeval.saga.annotations.QueryParameter;
import io.primeval.saga.annotations.intercept.InterceptAction;
import io.primeval.saga.http.protocol.HttpRequest;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.interception.action.ActionInterceptor;
import io.primeval.saga.interception.action.ActionInterceptorManager;
import io.primeval.saga.interception.action.MissingInterceptorException;
import io.primeval.saga.parameter.HttpParameterConverter;
import io.primeval.saga.router.Route;
import io.primeval.saga.router.RouterAction;
import io.primeval.saga.router.exception.ExceptionRecoveryHandler;
import io.primeval.saga.serdes.serializer.Serializable;

@Component
public final class ControllerRouteFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerRouteFinder.class);

    private ControllerRouteProvider controllerRouteProvider;
    private HttpParameterConverter httpParameterConverter;

    private ActionInterceptorManager actionInterceptorManager;
    private ExceptionRecoveryHandler exceptionRecoveryHandler;

    @Activate
    public void activate(BundleContext bundleContext) {
        controllerRouteProvider = new ControllerRouteProvider(bundleContext, this::routerActions);
        controllerRouteProvider.open();
    }

    @Deactivate
    public void deactivate() {
        controllerRouteProvider.close();
    }

    @Reference
    public void setHttpParamConverter(HttpParameterConverter httpParameterConverter) {
        this.httpParameterConverter = httpParameterConverter;
    }

    @Reference
    public void setActionInterceptorManager(ActionInterceptorManager actionInterceptorManager) {
        this.actionInterceptorManager = actionInterceptorManager;
    }

    @Reference
    public void setExceptionRecoveryHandler(ExceptionRecoveryHandler exceptionRecoveryHandler) {
        this.exceptionRecoveryHandler = exceptionRecoveryHandler;
    }

    public ActionFunction createInvocationHandler(MethodActionKey actionKey, Method m, Object target,
            Route route, List<String> routePattern, Function<Object, Promise<Result<?>>> wrap, TypeTag resultTypeTag)
            throws NoSuchMethodException, IllegalAccessException {

        Iterator<Annotation> interceptAnnotations = Stream.of(m.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(InterceptAction.class))
                .iterator();

        MethodHandle methodHandle = MethodHandles.publicLookup().findVirtual(m.getDeclaringClass(), m.getName(),
                MethodType.methodType(m.getReturnType(), m.getParameterTypes())).bindTo(target);
        List<Function<Context, Promise<?>>> inject = new ArrayList<>(m.getParameterCount());
        for (Parameter parameter : m.getParameters()) {

            if (parameter.getType() == HttpRequest.class) {
                Function<Context, Promise<?>> fun = context -> {
                    return Promises.resolved(context.request());
                };
                inject.add(fun);
                continue;
            }

            if (parameter.getType() == Context.class) {
                Function<Context, Promise<?>> fun = Promises::resolved;
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
                // Auto-detect from param name
                String paramName = pp.value().isEmpty() ? parameter.getName() : pp.value();

                String expectedPattern = '{' + paramName + '}';

                int patternIndex = findPatternIndex(routePattern, expectedPattern);

                if (patternIndex == -1) {
                    Function<Context, Promise<?>> fun = context -> {
                        return Promises.failed(new NoSuchElementException("no such path parameter named " + paramName));
                    };
                    inject.add(fun);
                } else {
                    Function<Context, Promise<?>> fun = context -> {
                        String paramValue = context.request().path.get(patternIndex);
                        if (parameter.getType() == String.class) {
                            return Promises.resolved(paramValue);
                        }
                        return httpParameterConverter.createParameter(paramValue,
                                TypeTag.of(parameter.getParameterizedType()));
                    };
                    inject.add(fun);
                }
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
        return wrapInterceptors(actionKey, Optional.of(route), interceptAnnotations,
                createActionFunction(wrap, resultTypeTag, methodHandle, inject));
    }

    private ActionFunction createActionFunction(Function<Object, Promise<Result<?>>> wrap, TypeTag resultTypeTag,
            MethodHandle methodHandle, List<Function<Context, Promise<?>>> inject) {
        return context -> {
            return Promises
                    .all(inject.stream().map(f -> f.apply(context)).collect(Collectors.toList()))
                    .flatMap(l -> PromiseHelper.wrap(() -> {
                        try {
                            return methodHandle.invokeWithArguments(l);
                        } catch (Throwable e) {
                            throw throwException(e);
                        }
                    }))
                    .flatMap(res -> wrap.apply(res).map(r -> setType(r, resultTypeTag)));
        };
    }

    private <T> ActionFunction wrapInterceptors(ActionKey actionKey, Optional<Route> route, Iterator<T> interceptAnnotations,
            ActionFunction sourceInvocation) {
        if (!interceptAnnotations.hasNext()) {
            return sourceInvocation;
        } else {
            T head = interceptAnnotations.next();
            return wrapInterceptor(actionKey, route, head,
                    wrapInterceptors(actionKey, route, interceptAnnotations, sourceInvocation));
        }

    }

    private <T> ActionFunction wrapInterceptor(ActionKey actionKey, Optional<Route> route, T interceptAnnotation,
            ActionFunction sourceInvocation) {
        @SuppressWarnings("unchecked")
        Class<T> interceptorType = (Class<T>) ((Annotation) interceptAnnotation).annotationType();
        ActionInterceptor<T> actionInterceptor = actionInterceptorManager.actionInterceptor(interceptorType);
        InterceptAction interceptAction = interceptorType.getAnnotation(InterceptAction.class);
        if (actionInterceptor == null) {
            if (interceptAction.required()) {
                return ActionFunction.failed(new MissingInterceptorException(interceptorType));
            } else {
                return sourceInvocation;
            }
        } else {
            ActionFunction nextFun = actionInterceptor.applyRecovery() ? exceptionRecoveryHandler.wrap(sourceInvocation, route)
                    : sourceInvocation;
            return actionInterceptor.wrap(nextFun, interceptAnnotation, actionKey);
        }
    }

    private int findPatternIndex(List<String> pathPattern, String expectedPattern) {
        for (int i = 0; i < pathPattern.size(); i++) {
            String pattern = pathPattern.get(i);
            if (expectedPattern.equals(pattern)) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException throwException(Throwable exception) throws T {
        throw (T) exception;
    }

    @SuppressWarnings("rawtypes")
    private <T> Result<?> setType(Result<T> res, TypeTag typeTag) {
        if (!res.content().isPresent() || typeTag.rawType() == Object.class) {
            return res;
        }
        Serializable<T> content = res.content().get();
        return content.explicitTypeTag().map(t -> res).orElseGet(() -> new Result<T>() {
            Optional<TypeTag<? extends T>> tt = Optional.of(typeTag);
            Serializable<T> contents = new Serializable<T>() {

                @Override
                public T value() {
                    return content.value();
                }

                @Override
                public Optional<TypeTag<? extends T>> explicitTypeTag() {
                    return tt;
                }

            };
            Optional<Serializable<T>> contentsWrap = Optional.of(contents);

            @Override
            public int statusCode() {
                return res.statusCode();
            }

            @Override
            public Map<String, List<String>> headers() {
                return res.headers();
            }

            @Override
            public Optional<Serializable<T>> content() {
                return contentsWrap;
            }
        });
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

            List<String> routePattern = RouteUtils.path(basePath + routeAnn.uri());
            List<String> routeURI = new ArrayList<>(routePattern.size());
            for (String routeURIPart : routePattern) {
                if (routeURIPart.startsWith("{") && routeURIPart.endsWith("}")) {
                    routeURI.add(".*");
                } else {
                    routeURI.add(routeURIPart);
                }
            }

            Route route = new io.primeval.saga.router.Route(routeAnn.method(),
                    routeURI);

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
                MethodActionKey actionKey = new MethodActionKey(m);

                ActionFunction actionInvocationHandler = createInvocationHandler(actionKey, m, controller, route, routePattern,
                        wrap, resultTypeTag);
                RouterAction boundAction = new RouterAction(route,
                        new Action(actionKey, actionInvocationHandler));

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
