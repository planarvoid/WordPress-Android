package com.soundcloud.android.cast;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class RemoteTrack {

    public abstract Optional<String> id();

    public abstract Urn urn();

    @JsonCreator
    public static RemoteTrack create(@JsonProperty("id") String id, @JsonProperty("urn") Urn urn) {
        return new AutoValue_RemoteTrack(Optional.fromNullable(id), urn);
    }

    public static RemoteTrack create(Urn urn) {
        return new AutoValue_RemoteTrack(Optional.absent(), urn);
    }

    public String getUrn() {
        return urn().toString();
    }
}
