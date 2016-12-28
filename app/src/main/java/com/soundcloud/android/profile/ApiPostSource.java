package com.soundcloud.android.profile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiPlaylistRepost;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiTrackRepost;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class ApiPostSource implements ApiEntityHolderSource {
    @JsonCreator
    public static ApiPostSource create(@JsonProperty("track_post") ApiTrackPost trackPost,
                                       @JsonProperty("track_repost") ApiTrackRepost trackRepost,
                                       @JsonProperty("playlist_post") ApiPlaylistPost playlistPost,
                                       @JsonProperty("playlist_repost") ApiPlaylistRepost playlistRepost) {
        return new AutoValue_ApiPostSource(Optional.fromNullable(trackPost), Optional.fromNullable(trackRepost), Optional.fromNullable(playlistPost), Optional.fromNullable(playlistRepost));
    }

    public abstract Optional<ApiTrackPost> getTrackPost();

    public abstract Optional<ApiTrackRepost> getTrackRepost();

    public abstract Optional<ApiPlaylistPost> getPlaylistPost();

    public abstract Optional<ApiPlaylistRepost> getPlaylistRepost();

    @Override
    public Optional<ApiEntityHolder> getEntityHolder() {
        if (getTrackPost().isPresent()) {
            return Optional.of(getTrackPost().get());
        } else if (getTrackRepost().isPresent()) {
            return Optional.of(getTrackRepost().get());
        } else if (getPlaylistPost().isPresent()) {
            return Optional.of(getPlaylistPost().get());
        } else if (getPlaylistRepost().isPresent()) {
            return Optional.of(getPlaylistRepost().get());
        } else {
            return Optional.absent();
        }
    }
}
