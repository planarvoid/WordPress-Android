package com.soundcloud.android.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

public class ApiPlayableSource implements ApiEntityHolderSource {

    @Nullable private final ApiTrack track;
    @Nullable private final ApiPlaylist playlist;

    public ApiPlayableSource(@JsonProperty("track") ApiTrack track,
                             @JsonProperty("playlist") ApiPlaylist playlist) {

        this.track = track;
        this.playlist = playlist;
    }

    public Optional<ApiEntityHolder> getEntityHolder() {
        if (track != null) {
            return Optional.<ApiEntityHolder>of(track);
        } else if (playlist != null) {
            return Optional.<ApiEntityHolder>of(playlist);
        } else {
            return Optional.absent();
        }
    }
}