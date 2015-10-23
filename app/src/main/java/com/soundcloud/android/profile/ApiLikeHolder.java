package com.soundcloud.android.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

public class ApiLikeHolder {

    @Nullable private final ApiTrack track;
    @Nullable private final ApiPlaylist playlist;

    public ApiLikeHolder(@JsonProperty("track") ApiTrack track,
                         @JsonProperty("playlist") ApiPlaylist playlist) {

        this.track = track;
        this.playlist = playlist;
    }

    Optional<PropertySetSource> getLike() {
        if (track != null) {
            return Optional.<PropertySetSource>of(track);
        } else if (playlist != null) {
            return Optional.<PropertySetSource>of(playlist);
        } else {
            return Optional.absent();
        }
    }

}
