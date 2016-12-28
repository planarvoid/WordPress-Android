package com.soundcloud.android.sync.posts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.java.optional.Optional;

public class ApiPostItem {

    private final Optional<ApiPost> trackPost;
    private final Optional<ApiRepost> trackRepost;
    private final Optional<ApiPost> playlistPost;
    private final Optional<ApiRepost> playlistRepost;

    public ApiPostItem(@JsonProperty("track_post") ApiPost trackPost,
                       @JsonProperty("track_repost") ApiRepost trackRepost,
                       @JsonProperty("playlist_post") ApiPost playlistPost,
                       @JsonProperty("playlist_repost") ApiRepost playlistRepost) {

        this.trackPost = Optional.fromNullable(trackPost);
        this.trackRepost = Optional.fromNullable(trackRepost);
        this.playlistPost = Optional.fromNullable(playlistPost);
        this.playlistRepost = Optional.fromNullable(playlistRepost);
    }

    public Optional<ApiPost> getTrackPost() {
        return trackPost;
    }

    public Optional<ApiRepost> getTrackRepost() {
        return trackRepost;
    }

    public Optional<ApiPost> getPlaylistPost() {
        return playlistPost;
    }

    public Optional<ApiRepost> getPlaylistRepost() {
        return playlistRepost;
    }

    public PostRecord getPostRecord() {
        if (trackPost.isPresent()) {
            return trackPost.get();

        } else if (trackRepost.isPresent()) {
            return trackRepost.get();

        } else if (playlistPost.isPresent()) {
            return playlistPost.get();

        } else {
            return playlistRepost.get();
        }
    }
}
