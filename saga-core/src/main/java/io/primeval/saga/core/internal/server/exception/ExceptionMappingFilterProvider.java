package io.primeval.saga.core.internal.server.exception;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.promise.Promise;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;

import io.primeval.codex.promise.PromiseHelper;
import io.primeval.saga.action.ActionFunction;
import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;
import io.primeval.saga.core.internal.server.Orderer;
import io.primeval.saga.core.internal.server.ServiceReferenceOrderer;
import io.primeval.saga.interception.request.RequestInterceptor;
import io.primeval.saga.router.Route;
import io.primeval.saga.router.exception.ExceptionRecovery;
import io.primeval.saga.router.exception.ExceptionRecoveryProvider;

// Separate injection to guarantee FIRST position.
@Component(immediate = true, service = ExceptionMappingFilterProvider.class)
public final class ExceptionMappingFilterProvider implements RequestInterceptor {

    private final Map<Class<? extends Throwable>, AtomicReference<SortedMap<Orderer<ExceptionRecoveryProvider<?>>, ExceptionRecovery<?>>>> mappers = Maps
            .newConcurrentMap();
    private volatile ImmutableListMultimap<Class<?>, ExceptionRecovery<?>> exceptionMatcher = ImmutableListMultimap
            .of();

    @Override
    public Promise<Result<?>> onRequest(Context context, ActionFunction function, Optional<Route> boundRoute) {
        return PromiseHelper.wrapPromise(() -> function.apply(context))
                .recoverWith(p -> handleRecovery(PromiseHelper.getFailure(p), context, boundRoute));

    }

    private <T extends Throwable> Promise<Result<?>> handleRecovery(T failure, Context context,
            Optional<Route> boundRoute) {
        Class<? extends Throwable> failureClazz = (Class<? extends Throwable>) failure.getClass();
        ImmutableList<ExceptionRecovery<?>> exceptionRecoveries = getRecoveries(failureClazz);

        if (exceptionRecoveries == null || exceptionRecoveries.isEmpty()) {
            return null;
        }

        return handleRemainingRecoveries(failure, context, boundRoute, exceptionRecoveries);
    }

    @SuppressWarnings("unchecked")
    private <T extends Throwable> Promise<Result<?>> handleRemainingRecoveries(T failure, Context context,
            Optional<Route> boundRoute, ImmutableList<ExceptionRecovery<?>> remainingRecoveries) {

        ExceptionRecovery<T> rec = (ExceptionRecovery<T>) remainingRecoveries.get(0); // non empty.
        ImmutableList<ExceptionRecovery<?>> tail = remainingRecoveries.subList(1, remainingRecoveries.size());

        Promise<Result<?>> res = PromiseHelper.wrapPromise(() -> rec.recover(failure, context, boundRoute));
        if (res == null) {
            // That recovery returned null, means no change
            return handleNextRecoveries(failure, context, boundRoute, tail);
        } else {
            return res.recoverWith(
                    p -> {
                        Throwable f = PromiseHelper.getFailure(p);
                        ImmutableList<ExceptionRecovery<?>> nextRecoveries = tail;
                        if (!Objects.equals(f, failure)) {
                            // otherwise get recoveries for new exception type, but filter with those already active
                            // to ensure termination
                            Class<? extends Throwable> failureClazz = (Class<? extends Throwable>) f.getClass();
                            ImmutableList<ExceptionRecovery<?>> exceptionRecoveries = getRecoveries(failureClazz);
                            nextRecoveries = exceptionRecoveries.stream()
                                    .filter(tail::contains).collect(ImmutableList.toImmutableList());
                            // If same error that couldn't be fixed, go to next recovery
                        } 
                        return handleNextRecoveries(f, context, boundRoute, nextRecoveries);
                    });
        }
    }

    private <T extends Throwable> Promise<Result<?>> handleNextRecoveries(T failure, Context context,
            Optional<Route> boundRoute,
            ImmutableList<ExceptionRecovery<?>> tail) {
        if (!tail.isEmpty()) {
            return handleRemainingRecoveries(failure, context, boundRoute, tail);
        } else {
            return null; // no more solution.
        }
    }

    private ImmutableList<ExceptionRecovery<?>> getRecoveries(Class<?> failureClazz) {
        ImmutableList<ExceptionRecovery<?>> recoveries;
        do {
            recoveries = exceptionMatcher.get(failureClazz);
            failureClazz = failureClazz.getSuperclass();
        } while (recoveries.isEmpty() && failureClazz != Object.class);
        return recoveries;
    }

    @Override
    public boolean matches(String uri) {
        return true;
    }

    public void addExceptionMapper(ExceptionRecoveryProvider<?> exceptionMapper,
            Orderer<ExceptionRecoveryProvider<?>> orderer) {
        Class<? extends Throwable> exceptionType = exceptionMapper.exceptionType();
        if (!Throwable.class.isAssignableFrom(exceptionType)) {
            // Can't happen from Java, but could through manufactured bytecode
            return;
        }

        mappers.computeIfAbsent(exceptionType, k -> new AtomicReference<>(ImmutableSortedMap.of()))
                .updateAndGet(prev -> {
                    return ImmutableSortedMap
                            .<Orderer<ExceptionRecoveryProvider<?>>, ExceptionRecovery<?>> naturalOrder()
                            .putAll(prev)
                            .put(orderer, exceptionMapper).build();
                });

        onMapperChange();
    }

    public void removeExceptionMapper(ExceptionRecoveryProvider<?> exceptionMapper,
            Orderer<ExceptionRecoveryProvider<?>> orderer) {
        Class<? extends Throwable> exceptionType = exceptionMapper.exceptionType();
        if (!Throwable.class.isAssignableFrom(exceptionType)) {
            // Can't happen from Java, but could through manufactured bytecode
            return;
        }

        Optional.ofNullable(mappers.get(exceptionType)).ifPresent(val -> val.getAndUpdate(current -> {
            return ImmutableSortedMap.copyOf(Maps.filterKeys(current, key -> key.compareTo(orderer) != 0));
        }));

        // Remove if we have no more element
        mappers.remove(exceptionType, ImmutableSortedMap.of());

        onMapperChange();
    }

    public void onMapperChange() {
        exceptionMatcher = buildExceptionMatcher();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addExceptionMapper(ExceptionRecoveryProvider<?> exceptionMapper,
            ServiceReference<ExceptionRecoveryProvider<?>> ref) {

        ServiceReferenceOrderer<ExceptionRecoveryProvider<?>> referenceOrderer = new ServiceReferenceOrderer<ExceptionRecoveryProvider<?>>(
                exceptionMapper, ref);

        addExceptionMapper(exceptionMapper, referenceOrderer);
    }

    public void removeExceptionMapper(ExceptionRecoveryProvider<?> exceptionMapper,
            ServiceReference<ExceptionRecoveryProvider<?>> ref) {

        ServiceReferenceOrderer<ExceptionRecoveryProvider<?>> referenceOrderer = new ServiceReferenceOrderer<ExceptionRecoveryProvider<?>>(
                exceptionMapper, ref);

        removeExceptionMapper(exceptionMapper, referenceOrderer);
    }

    private ImmutableListMultimap<Class<?>, ExceptionRecovery<?>> buildExceptionMatcher() {

        ImmutableListMultimap.Builder<Class<?>, ExceptionRecovery<?>> exceptionMatcherBuilder = ImmutableListMultimap
                .builder();

        for (Entry<Class<? extends Throwable>, AtomicReference<SortedMap<Orderer<ExceptionRecoveryProvider<?>>, ExceptionRecovery<?>>>> entry : mappers
                .entrySet()) {

            Class<? extends Throwable> exceptionType = entry.getKey();

            List<ExceptionRecovery<?>> exceptionRecovBuilder = new ArrayList<>();

            exceptionRecovBuilder.addAll(entry.getValue().get().values());

            Class<?> superclass = exceptionType.getSuperclass();
            while (superclass != Object.class) {
                Optional.ofNullable(mappers.get(superclass))
                        .ifPresent(recoveries -> exceptionRecovBuilder.addAll(recoveries.get().values()));

                superclass = superclass.getSuperclass();
            }

            exceptionMatcherBuilder.putAll(exceptionType, exceptionRecovBuilder);

        }
        return exceptionMatcherBuilder.build();

    }

}
