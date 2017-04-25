package com.soundcloud.android.mrlocallocal.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;


import java.util.Map;

@AutoValue
public abstract class SpecEvent {
    public abstract String name();
    public abstract boolean optional();
    public abstract Map<String, Object> params();
    public abstract Optional<String> version();

    // Don't be fooled by the Jackson JSON annotations here, this is YAML!
    @JsonCreator
    public static SpecEvent create(@JsonProperty("name") String name,
                                   @JsonProperty("optional") boolean optional,
                                   @JsonProperty("params") Map<String, Object> params,
                                   @JsonProperty("version") String version) {
        return new AutoValue_SpecEvent(name, optional, params, Optional.fromNullable(version));
    }
}
