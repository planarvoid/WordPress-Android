package com.soundcloud.android.mrlocallocal.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class Spec {
    public abstract List<String> whitelistedEvents();
    public abstract List<SpecEvent> expectedEvents();

    // Don't be fooled by the Jackson JSON annotations here, this is YAML!
    @JsonCreator
    public static Spec create(@JsonProperty("whitelisted_events") List<String> whitelistedEvents,
                              @JsonProperty("expected_events") List<SpecEvent> expectedEvents) {
        return new AutoValue_Spec(whitelistedEvents, expectedEvents);
    }
}
