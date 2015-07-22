package com.soundcloud.android.sync.posts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.java.collections.PropertySet;

public class ApiPostItem implements PropertySetSource {

    private final ApiPost trackPost;
    private final ApiRepost trackRepost;
    private final ApiPost playlistPost;
    private final ApiRepost playlistRepost;

    public ApiPostItem(@JsonProperty("track_post") ApiPost trackPost,
                       @JsonProperty("track_repost") ApiRepost trackRepost,
                       @JsonProperty("playlist_post") ApiPost playlistPost,
                       @JsonProperty("playlist_repost") ApiRepost playlistRepost) {

        this.trackPost = trackPost;
        this.trackRepost = trackRepost;
        this.playlistPost = playlistPost;
        this.playlistRepost = playlistRepost;
    }


    @Override
    public PropertySet toPropertySet() {
        if (trackPost != null) {
            return trackPost.toPropertySet();

        } else if (trackRepost != null) {
            return trackRepost.toPropertySet();

        } else if (playlistPost != null) {
            return playlistPost.toPropertySet();

        } else {
            return playlistRepost.toPropertySet();
        }
    }
}
