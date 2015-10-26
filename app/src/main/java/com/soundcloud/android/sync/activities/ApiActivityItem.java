package com.soundcloud.android.sync.activities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiActivityItem {

    private final ApiTrackLikeActivity trackLike;
    private final ApiTrackRepostActivity trackRepost;
    private final ApiTrackCommentActivity trackComment;
    private final ApiPlaylistLikeActivity playlistLike;
    private final ApiPlaylistRepostActivity playlistRepost;
    private final ApiUserFollowActivity userFollow;
    private final ApiUserMentionActivity userMention;

    @JsonCreator
    public ApiActivityItem(@JsonProperty("track_like") ApiTrackLikeActivity trackLike,
                           @JsonProperty("track_repost") ApiTrackRepostActivity trackRepost,
                           @JsonProperty("track_comment") ApiTrackCommentActivity trackComment,
                           @JsonProperty("playlist_like") ApiPlaylistLikeActivity playlistLike,
                           @JsonProperty("playlist_repost") ApiPlaylistRepostActivity playlistRepost,
                           @JsonProperty("user_follow") ApiUserFollowActivity userFollow,
                           @JsonProperty("user_mention") ApiUserMentionActivity userMention) {
        this.trackLike = trackLike;
        this.trackRepost = trackRepost;
        this.trackComment = trackComment;
        this.playlistLike = playlistLike;
        this.playlistRepost = playlistRepost;
        this.userFollow = userFollow;
        this.userMention = userMention;
    }

    public ApiTrackLikeActivity getTrackLike() {
        return trackLike;
    }

    public ApiTrackRepostActivity getTrackRepost() {
        return trackRepost;
    }

    public ApiTrackCommentActivity getTrackComment() {
        return trackComment;
    }

    public ApiPlaylistLikeActivity getPlaylistLike() {
        return playlistLike;
    }

    public ApiPlaylistRepostActivity getPlaylistRepost() {
        return playlistRepost;
    }

    public ApiUserFollowActivity getUserFollow() {
        return userFollow;
    }

    public ApiUserMentionActivity getUserMention() {
        return userMention;
    }
}
