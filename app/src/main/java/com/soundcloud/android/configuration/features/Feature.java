package com.soundcloud.android.configuration.features;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.java.objects.MoreObjects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Feature {

    public final String name;
    public final boolean enabled;
    public final List<String> plans;

    @JsonCreator
    public Feature(@JsonProperty("name") String name,
                   @JsonProperty("enabled") boolean enabled,
                   @JsonProperty("plans") List<String> plans) {
        this.name = name;
        this.enabled = enabled;
        this.plans = Collections.unmodifiableList(plans != null ? plans : new ArrayList<String>(0));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Feature that = (Feature) o;
        return MoreObjects.equal(name, that.name)
                && MoreObjects.equal(enabled, that.enabled);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(name, enabled);
    }

}
