package com.soundcloud.android.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

public class ApiPlayableHolder implements PropertySetSourceHolder {

    @Nullable private final ApiTrack track;
    @Nullable private final ApiPlaylist playlist;

    public ApiPlayableHolder(@JsonProperty("track") ApiTrack track,
                             @JsonProperty("playlist") ApiPlaylist playlist) {

        this.track = track;
        this.playlist = playlist;
    }

    public Optional<PropertySetSource> getItem() {
        if (track != null) {
            return Optional.<PropertySetSource>of(track);
        } else if (playlist != null) {
            return Optional.<PropertySetSource>of(playlist);
        } else {
            return Optional.absent();
        }
    }

}
