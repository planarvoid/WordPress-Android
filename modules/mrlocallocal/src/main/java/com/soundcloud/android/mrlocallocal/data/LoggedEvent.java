package com.soundcloud.android.mrlocallocal.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Map;

@AutoValue
public abstract class LoggedEvent {
    public abstract String event();
    public abstract Map<String, Object> payload();
    public abstract String version();

    @JsonCreator
    public static LoggedEvent create(@JsonProperty("event") String event,
                                     @JsonProperty("payload") Map<String, Object> payload,
                                     @JsonProperty("version") String version) {
        return new AutoValue_LoggedEvent(event, payload, version);
    }
}
