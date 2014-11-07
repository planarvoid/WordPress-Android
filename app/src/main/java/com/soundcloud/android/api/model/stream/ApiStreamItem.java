package com.soundcloud.android.api.model.stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;

public class ApiStreamItem {

    private ApiPromotedTrack apiPromotedTrack;
    private ApiTrackPost apiTrackPost;
    private ApiTrackRepost apiTrackRepost;
    private ApiPlaylistPost apiPlaylistPost;
    private ApiPlaylistRepost apiPlaylistRepost;

    /**
     * Unfortunately, you can only have 1 constructor responsible for property based construction
     * In practice, only one of these will be non-null, but I still think its better than setters.
     * - JS
     */
    @JsonCreator
    public ApiStreamItem(@JsonProperty("promoted_track") ApiPromotedTrack apiPromotedTrack,
                         @JsonProperty("track_post") ApiTrackPost apiTrackPost,
                         @JsonProperty("track_repost") ApiTrackRepost apiTrackRepost,
                         @JsonProperty("playlist_post") ApiPlaylistPost apiPlaylistPost,
                         @JsonProperty("playlist_repost") ApiPlaylistRepost apiPlaylistRepost) {
        this.apiPromotedTrack = apiPromotedTrack;
        this.apiTrackPost = apiTrackPost;
        this.apiTrackRepost = apiTrackRepost;
        this.apiPlaylistPost = apiPlaylistPost;
        this.apiPlaylistRepost = apiPlaylistRepost;
    }

    @VisibleForTesting
    public ApiStreamItem(ApiTrackPost apiTrackPost) {
        this.apiTrackPost = apiTrackPost;
    }

    @VisibleForTesting
    public ApiStreamItem(ApiPromotedTrack apiPromotedTrack) {
        this.apiPromotedTrack = apiPromotedTrack;
    }

    @VisibleForTesting
    public ApiStreamItem(ApiTrackRepost apiTrackRepost) {
        this.apiTrackRepost = apiTrackRepost;
    }

    @VisibleForTesting
    public ApiStreamItem(ApiPlaylistPost apiPlaylistPost) {
        this.apiPlaylistPost = apiPlaylistPost;
    }

    @VisibleForTesting
    public ApiStreamItem(ApiPlaylistRepost apiPlaylistRepost) {
        this.apiPlaylistRepost = apiPlaylistRepost;
    }

    public boolean isPromotedStreamItem(){
        return apiPromotedTrack != null;
    }

    public Optional<ApiTrack> getTrack() {
        if (apiTrackPost != null){
            return Optional.of(apiTrackPost.getApiTrack());

        } else if (apiTrackRepost != null) {
            return Optional.of(apiTrackRepost.getApiTrack());

        } else {
            return Optional.absent();
        }
    }

    public Optional<ApiPlaylist> getPlaylist() {
        if (apiPlaylistPost != null){
            return Optional.of(apiPlaylistPost.getApiPlaylist());

        } else if (apiPlaylistRepost != null) {
            return Optional.of(apiPlaylistRepost.getApiPlaylist());

        } else {
            return Optional.absent();
        }
    }

    public Optional<ApiUser> getReposter() {
        if (apiTrackRepost != null){
            return Optional.of(apiTrackRepost.getReposter());

        } else if (apiPlaylistRepost != null) {
            return Optional.of(apiPlaylistRepost.getReposter());

        } else {
            return Optional.absent();
        }
    }

    public long getCreatedAtTime() {

        if (apiTrackPost != null){
            return apiTrackPost.getApiTrack().getCreatedAt().getTime();

        } else if (apiTrackRepost != null) {
            return apiTrackRepost.getCreatedAtTime();

        } else if (apiPlaylistPost != null){
            return apiPlaylistPost.getApiPlaylist().getCreatedAt().getTime();

        } else if (apiPlaylistRepost != null) {
            return apiPlaylistRepost.getCreatedAtTime();

        } else {
            throw new IllegalArgumentException("Unknown stream item type when fecthing creation date");
        }
    }
}
