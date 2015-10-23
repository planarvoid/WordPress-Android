package com.soundcloud.android.sync.activities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiActivityItem {

    private final ApiTrackLike trackLike;
    private final ApiTrackRepost trackRepost;
    private final ApiTrackComment trackComment;
    private final ApiPlaylistLike playlistLike;
    private final ApiPlaylistRepost playlistRepost;
    private final ApiUserFollow userFollow;
    private final ApiUserMention userMention;

    @JsonCreator
    public ApiActivityItem(@JsonProperty("track_like") ApiTrackLike trackLike,
                           @JsonProperty("track_repost") ApiTrackRepost trackRepost,
                           @JsonProperty("track_comment") ApiTrackComment trackComment,
                           @JsonProperty("playlist_like") ApiPlaylistLike playlistLike,
                           @JsonProperty("playlist_repost") ApiPlaylistRepost playlistRepost,
                           @JsonProperty("user_follow") ApiUserFollow userFollow,
                           @JsonProperty("user_mention") ApiUserMention userMention) {
        this.trackLike = trackLike;
        this.trackRepost = trackRepost;
        this.trackComment = trackComment;
        this.playlistLike = playlistLike;
        this.playlistRepost = playlistRepost;
        this.userFollow = userFollow;
        this.userMention = userMention;
    }

    public ApiTrackLike getTrackLike() {
        return trackLike;
    }

    public ApiTrackRepost getTrackRepost() {
        return trackRepost;
    }

    public ApiTrackComment getTrackComment() {
        return trackComment;
    }

    public ApiPlaylistLike getPlaylistLike() {
        return playlistLike;
    }

    public ApiPlaylistRepost getPlaylistRepost() {
        return playlistRepost;
    }

    public ApiUserFollow getUserFollow() {
        return userFollow;
    }

    public ApiUserMention getUserMention() {
        return userMention;
    }
}
