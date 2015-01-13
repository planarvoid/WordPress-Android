package com.soundcloud.android.configuration.features;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class Feature {
    public final String name;
    public final boolean enabled;

    @JsonCreator
    public Feature(@JsonProperty("name") String name, @JsonProperty("enabled") boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Feature that = (Feature) o;
        return Objects.equal(name, that.name)
                && Objects.equal(enabled, that.enabled);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, enabled);
    }
}
