package com.soundcloud.android.profile;

import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Banana;
import com.soundcloud.java.optional.Optional;

public class ApiPlayableHolder implements BananaHolder {

    @Nullable private final ApiTrack track;
    @Nullable private final ApiPlaylist playlist;

    public ApiPlayableHolder(@JsonProperty("track") ApiTrack track,
                             @JsonProperty("playlist") ApiPlaylist playlist) {

        this.track = track;
        this.playlist = playlist;
    }

    public Optional<Banana> getItem() {
        if (track != null) {
            return Optional.<Banana>of(track);
        } else if (playlist != null) {
            return Optional.<Banana>of(playlist);
        } else {
            return Optional.absent();
        }
    }

}
