package io.primeval.saga.router;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.stream.Collectors;

import io.primeval.saga.http.protocol.HttpMethod;

public final class Route {

    public final HttpMethod method;
    public final List<String> pathPattern;

    public Route(HttpMethod method, List<String> pathPattern) {
        this.method = method;
        this.pathPattern = pathPattern;
    }

    public static Route create(HttpMethod method, List<String> pathPattern) {
        return new Route(method, pathPattern);
    }

    public boolean matches(HttpMethod method, List<String> path) {
        if (this.method != method) {
            return false;
        }
        int size = path.size();
        if (size != pathPattern.size()) {
            return false;
        }
        ListIterator<String> pathIt = path.listIterator();
        ListIterator<String> pathPatIt = pathPattern.listIterator();
        while (pathIt.hasNext()) {
            String pathSeg = pathIt.next();
            String pathPatSeg = pathPatIt.next();
            if (!pathSeg.matches(pathPatSeg)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return method.name() + " " + pathPattern.stream().collect(Collectors.joining("/"));
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, pathPattern);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Route other = (Route) obj;
        return Objects.equals(method, other.method) && Objects.equals(pathPattern, other.pathPattern);
    }

}
