package com.soundcloud.android.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiPlaylistRepost;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiTrackRepost;
import com.soundcloud.android.model.PropertySetSource;

import android.support.annotation.Nullable;

public class ApiPostHolder {

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

    boolean isValid() {
        return getPost() != null;
    }

    @Nullable
    PropertySetSource getPost() {
        if (trackPost != null) {
            return trackPost;
        } else if (trackRepost != null) {
            return trackRepost;
        } else if (playlistPost != null) {
            return playlistPost;
        } else if (playlistRepost != null) {
            return playlistRepost;
        } else {
            return null;
        }
    }

}
