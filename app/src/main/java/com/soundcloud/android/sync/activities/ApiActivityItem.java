package com.soundcloud.android.sync.activities;

import static com.soundcloud.java.collections.MoreArrays.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.PlaylistHolder;
import com.soundcloud.android.model.TrackHolder;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

@AutoValue
public abstract class ApiActivityItem {

    @Nullable private ApiActivity activity;

    @JsonCreator
    public static ApiActivityItem create(@JsonProperty("track_like") ApiTrackLikeActivity trackLike,
                                         @JsonProperty("track_repost") ApiTrackRepostActivity trackRepost,
                                         @JsonProperty("track_comment") ApiTrackCommentActivity trackComment,
                                         @JsonProperty("playlist_like") ApiPlaylistLikeActivity playlistLike,
                                         @JsonProperty("playlist_repost") ApiPlaylistRepostActivity playlistRepost,
                                         @JsonProperty("user_follow") ApiUserFollowActivity userFollow) {
        return builder()
                .trackLike(trackLike)
                .trackRepost(trackRepost)
                .trackComment(trackComment)
                .playlistLike(playlistLike)
                .playlistRepost(playlistRepost)
                .userFollow(userFollow)
                .build();
    }

    public static ApiActivityItem.Builder builder() {
        return new AutoValue_ApiActivityItem.Builder();
    }

    @Nullable
    protected abstract ApiTrackLikeActivity trackLike();

    @Nullable
    protected abstract ApiTrackRepostActivity trackRepost();

    @Nullable
    protected abstract ApiTrackCommentActivity trackComment();

    @Nullable
    protected abstract ApiPlaylistLikeActivity playlistLike();

    @Nullable
    protected abstract ApiPlaylistRepostActivity playlistRepost();

    @Nullable
    protected abstract ApiUserFollowActivity userFollow();

    // TODO: filter out invalid items
    public boolean isValid() {
        return this.activity != null;
    }

    public Optional<ApiTrackCommentActivity> getTrackComment() {
        return Optional.fromNullable(trackComment());
    }

    public Optional<ApiUserFollowActivity> getFollow() {
        return Optional.fromNullable(userFollow());
    }

    public Optional<ApiEngagementActivity> getLike() {
        return Optional.fromNullable(firstNonNull(trackLike(), playlistLike()));
    }

    public Optional<ApiEngagementActivity> getRepost() {
        return Optional.fromNullable(firstNonNull(trackRepost(), playlistRepost()));
    }

    public Optional<UserRecord> getUser() {
        if (activity != null) {
            return Optional.fromNullable(activity.getUser());
        }
        return Optional.absent();
    }

    public Optional<TrackRecord> getTrack() {
        if (activity instanceof TrackHolder) {
            return Optional.fromNullable(((TrackHolder) activity).getTrack());
        }
        return Optional.absent();
    }

    public Optional<PlaylistRecord> getPlaylist() {
        if (activity instanceof PlaylistHolder) {
            return Optional.fromNullable(((PlaylistHolder) activity).getPlaylist());
        }
        return Optional.absent();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder trackLike(ApiTrackLikeActivity trackLikeActivity);

        public abstract Builder trackRepost(ApiTrackRepostActivity trackRepostActivity);

        public abstract Builder trackComment(ApiTrackCommentActivity trackCommentActivity);

        public abstract Builder playlistLike(ApiPlaylistLikeActivity playlistLikeActivity);

        public abstract Builder playlistRepost(ApiPlaylistRepostActivity playlistRepostActivity);

        public abstract Builder userFollow(ApiUserFollowActivity userFollowActivity);

        protected abstract ApiActivityItem autoBuild();

        public ApiActivityItem build() {
            final ApiActivityItem apiActivityItem = autoBuild();
            // only a single activity is only ever non-null, since this is just a wrapper class
            apiActivityItem.activity = firstNonNull(
                    apiActivityItem.trackLike(),
                    apiActivityItem.trackRepost(),
                    apiActivityItem.trackComment(),
                    apiActivityItem.playlistLike(),
                    apiActivityItem.playlistRepost(),
                    apiActivityItem.userFollow());
            return apiActivityItem;
        }
    }
}
