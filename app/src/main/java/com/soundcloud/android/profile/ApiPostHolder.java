package com.soundcloud.android.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiPlaylistRepost;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiTrackRepost;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

public class ApiPostHolder implements PropertySetSourceHolder {

    @Nullable private final ApiTrackPost trackPost;
    @Nullable private final ApiTrackRepost trackRepost;
    @Nullable private final ApiPlaylistPost playlistPost;
    @Nullable private final ApiPlaylistRepost playlistRepost;

    public ApiPostHolder(@JsonProperty("track_post") ApiTrackPost trackPost,
                         @JsonProperty("track_repost") ApiTrackRepost trackRepost,
                         @JsonProperty("playlist_post") ApiPlaylistPost playlistPost,
                         @JsonProperty("playlist_repost") ApiPlaylistRepost playlistRepost) {

        this.trackPost = trackPost;
        this.trackRepost = trackRepost;
        this.playlistPost = playlistPost;
        this.playlistRepost = playlistRepost;
    }

    public Optional<PropertySetSource> getItem() {
        if (trackPost != null) {
            return Optional.<PropertySetSource>of(trackPost);
        } else if (trackRepost != null) {
            return Optional.<PropertySetSource>of(trackRepost);
        } else if (playlistPost != null) {
            return Optional.<PropertySetSource>of(playlistPost);
        } else if (playlistRepost != null) {
            return Optional.<PropertySetSource>of(playlistRepost);
        } else {
            return Optional.absent();
        }
    }

}
