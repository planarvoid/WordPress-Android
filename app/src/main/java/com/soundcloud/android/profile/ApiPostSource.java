package com.soundcloud.android.profile;

import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiPlaylistRepost;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiTrackRepost;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.java.optional.Optional;

public class ApiPostSource implements ApiEntityHolderSource {
    @Nullable private final ApiTrackPost trackPost;
    @Nullable private final ApiTrackRepost trackRepost;
    @Nullable private final ApiPlaylistPost playlistPost;
    @Nullable private final ApiPlaylistRepost playlistRepost;

    public ApiPostSource(@JsonProperty("track_post") ApiTrackPost trackPost,
                         @JsonProperty("track_repost") ApiTrackRepost trackRepost,
                         @JsonProperty("playlist_post") ApiPlaylistPost playlistPost,
                         @JsonProperty("playlist_repost") ApiPlaylistRepost playlistRepost) {

        this.trackPost = trackPost;
        this.trackRepost = trackRepost;
        this.playlistPost = playlistPost;
        this.playlistRepost = playlistRepost;
    }

    public Optional<ApiEntityHolder> getEntityHolder() {
        if (trackPost != null) {
            return Optional.<ApiEntityHolder>of(trackPost);
        } else if (trackRepost != null) {
            return Optional.<ApiEntityHolder>of(trackRepost);
        } else if (playlistPost != null) {
            return Optional.<ApiEntityHolder>of(playlistPost);
        } else if (playlistRepost != null) {
            return Optional.<ApiEntityHolder>of(playlistRepost);
        } else {
            return Optional.absent();
        }
    }

}
