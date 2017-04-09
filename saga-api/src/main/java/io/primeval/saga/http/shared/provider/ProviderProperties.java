package io.primeval.saga.http.shared.provider;

public final class ProviderProperties {
    public static final String PROVIDER_PROPERTY = "saga.provider";

    public final String provider;

    public ProviderProperties(String provider) {
        this.provider = provider;
    }

}