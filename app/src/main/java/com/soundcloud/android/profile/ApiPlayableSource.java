package com.soundcloud.android.profile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class ApiPlayableSource implements ApiEntityHolderSource {

    @JsonCreator
    public static ApiPlayableSource create(@JsonProperty("track") ApiTrack track,
                                           @JsonProperty("playlist") ApiPlaylist playlist) {
        return new AutoValue_ApiPlayableSource(Optional.fromNullable(track), Optional.fromNullable(playlist));
    }

    public abstract Optional<ApiTrack> getTrack();

    public abstract Optional<ApiPlaylist> getPlaylist();

    @Override
    public Optional<ApiEntityHolder> getEntityHolder() {
        if (getTrack().isPresent()) {
            return Optional.of(getTrack().get());
        } else if (getPlaylist().isPresent()) {
            return Optional.of(getPlaylist().get());
        } else {
            return Optional.absent();
        }
    }
}
