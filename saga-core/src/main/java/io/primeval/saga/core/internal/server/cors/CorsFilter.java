package io.primeval.saga.core.internal.server.cors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

import com.google.common.base.Joiner;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.action.ActionFunction;
import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;
import io.primeval.saga.guava.ImmutableResult;
import io.primeval.saga.http.protocol.HeaderNames;
import io.primeval.saga.http.protocol.HttpMethod;
import io.primeval.saga.http.protocol.HttpRequest;
import io.primeval.saga.http.protocol.HttpUtils;
import io.primeval.saga.router.Route;
import io.primeval.saga.router.Router;
import io.primeval.saga.router.filter.RouteFilterProvider;

@Component(configurationPid = "saga.cors.filter", configurationPolicy = ConfigurationPolicy.REQUIRE)
public final class CorsFilter implements RouteFilterProvider {

    private static final Pattern PATTERN = Pattern.compile(".*");

    private Router router;

    private CorsFilterConfig config;

    @Activate
    public void activate(CorsFilterConfig config) {
        this.config = config;
    }

    @Reference
    public void setRouter(Router router) {
        this.router = router;
    }

    @Override
    public Promise<Result<?>> call(Context context, ActionFunction actionFunction, Optional<Route> boundRoute) {
        // Is CORS required?
        List<String> originHeaders = context.request().headers.get(HeaderNames.ORIGIN);

        String originHeader = (originHeaders != null && !originHeaders.isEmpty()) ? originHeaders.get(0).toLowerCase()
                : null;

        // If not Preflight
        if (context.request().method != HttpMethod.OPTIONS) {
            return retrieveAndReturnResult(context, actionFunction, originHeader);
        }

        // OPTIONS route exists, don't use filter! (might manually implement
        // CORS?)
        if (boundRoute.isPresent()) {
            return actionFunction.apply(context);
        }

        // Try "Preflight"

        // Find existing methods for other routes
        Promise<Collection<Route>> routesPms = router.getRoutes();

        return routesPms.flatMap(routes -> {
            return preflight(context, actionFunction, originHeader, routes);
        });

    }

    @SuppressWarnings("unchecked")
    private Promise<Result<?>> preflight(Context context, ActionFunction actionFunction, String originHeader,
            Collection<Route> routes) {
        HttpRequest request = context.request();
        List<String> methods = new ArrayList<>(4); // expect POST PUT GET DELETE
        for (Route r : routes) {
            if (r.matches(r.method, request.path)) {
                methods.add(r.method.name());
            }
        }

        // If there's none, proceed to 404
        if (methods.isEmpty()) {
            return actionFunction.apply(context);
        }

        String requestMethod = HttpUtils.getHeader(request, HeaderNames.ACCESS_CONTROL_REQUEST_METHOD).orElse(null);

        // If it's not a CORS request, just proceed!
        if (originHeader == null || requestMethod == null) {
            return actionFunction.apply(context);
        }

        ImmutableResult.Builder res = ImmutableResult.builder(); // setup result

        if (!methods.contains(requestMethod.toUpperCase())) {
            res = res.withStatusCode(401).setValue("No such method for this route")
                    .withExplicitType(TypeTag.of(String.class));
        }

        Integer maxAge = config.max_age();
        if (maxAge != null) {
            res = res.withHeader(HeaderNames.ACCESS_CONTROL_MAX_AGE, String.valueOf(maxAge));
        }

        // Otherwise we should be return OK withHeader the appropriate headers.

        String exposedHeaders = getExposedHeadersHeader();
        String allowedHosts = getAllowedHostsHeader(originHeader);

        String allowedMethods = Joiner.on(", ").join(methods);

        res = res.withHeader(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, allowedHosts)
                .withHeader(HeaderNames.ACCESS_CONTROL_ALLOW_METHODS, allowedMethods)
                .withHeader(HeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, exposedHeaders);
        if (config.allow_credentials()) {
            res = res.withHeader(HeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        return Promises.resolved(res.build());
    }

    protected Promise<Result<?>> retrieveAndReturnResult(Context context, ActionFunction actionFunction,
            String originHeader) {
        Promise<Result<?>> resultPms = actionFunction.apply(context);

        // Is it actually a CORS request?
        if (originHeader != null) {
            resultPms = resultPms.map(result -> {
                ImmutableResult.Builder resBuilder = ImmutableResult.copyOf(result);
                String allowedHosts = getAllowedHostsHeader(originHeader);
                resBuilder = resBuilder.withHeader(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, allowedHosts);
                if (config.allow_credentials()) {
                    resBuilder = resBuilder.withHeader(HeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
                }
                if (config.exposed_headers().length > 0) {
                    resBuilder = resBuilder.withHeader(HeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS, getExposedHeadersHeader());
                }
                return resBuilder.build();
            });

        }

        return resultPms;
    }

    private String getExposedHeadersHeader() {
        return Stream.of(config.exposed_headers()).collect(Collectors.joining(", "));
    }

    private String getAllowedHostsHeader(final String origin) {
        final List<String> allowedHosts = config.allowed_hosts() != null ? Arrays.asList(config.allowed_hosts())
                : Collections.emptyList();
        // If wildcard is used, only return the request supplied origin
        if (config.allow_credentials() && allowedHosts.contains("*")) {
            return origin;
        } else {
            return Joiner.on(", ").join(allowedHosts);
        }
    }

    @Override
    public boolean matches(String pattern) {
        return PATTERN.matcher(pattern).matches();
    }

}
