package io.primeval.saga.http.client.header;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class HeaderAttributeEntry {
    public final String name;
    public final List<String> values;

    HeaderAttributeEntry(String name, List<String> values) {
        this.name = name;
        this.values = Collections.unmodifiableList(values);
    }

    public static HeaderAttributeEntry create(String name, String value) {
        return new HeaderAttributeEntry(name, Collections.singletonList(value));
    }

    public static HeaderAttributeEntry create(String name, List<String> values) {
        return new HeaderAttributeEntry(name, values);
    }

    @Override
    public String toString() {
        return HeaderAttributeEntry.class.getName() + "{name=" + name + ",values=" + values + "}";
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, values);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        HeaderAttributeEntry other = (HeaderAttributeEntry) obj;
        return Objects.equals(this.name, other.name) && Objects.equals(this.values, other.values);
    }

}