package io.primeval.saga.core.internal.server;

import org.osgi.framework.ServiceReference;

public final class ServiceReferenceOrderer<E> implements Orderer<E> {
    private static final String KEY = ServiceReferenceOrderer.class.getName();

    private final E element;
    private final ServiceReference<E> ref;

    public ServiceReferenceOrderer(E element, ServiceReference<E> ref) {
        this.element = element;
        this.ref = ref;
    }

    @Override
    public int compareTo(Orderer<E> o) {
        int res = KEY.compareTo(o.getClass().getName());
        if (res != 0) {
            return res;
        }
        ServiceReferenceOrderer<?> other = (ServiceReferenceOrderer<?>) o;
        return ref.compareTo(other.ref);
    }

    @Override
    public E element() {
        return element;
    }

}
