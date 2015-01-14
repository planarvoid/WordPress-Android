package com.soundcloud.android.api.model.stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;

import java.util.Collections;
import java.util.List;

public class ApiStreamItem {

    private static final long PROMOTED_CREATION_DATE = Long.MAX_VALUE;

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

        } else if (apiPromotedTrack != null) {
            return Optional.of(apiPromotedTrack.getApiTrack());

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

    public Optional<ApiUser> getPromoter(){
        if (apiPromotedTrack != null && apiPromotedTrack.getPromoter() != null){
            return Optional.of(apiPromotedTrack.getPromoter());
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

        } else if (apiPromotedTrack != null) {
            return PROMOTED_CREATION_DATE;

        } else {
            throw new IllegalArgumentException("Unknown stream item type when fecthing creation date");
        }
    }

    public Optional<Urn> getPromotedUrn() {
        if (apiPromotedTrack != null){
            return Optional.of(apiPromotedTrack.getUrn());
        } else {
            return Optional.absent();
        }
    }

    public List<String> getTrackingProfileClickedUrls() {
        return apiPromotedTrack == null ? Collections.<String>emptyList() : apiPromotedTrack.getTrackingProfileClickedUrls();
    }

    public List<String> getTrackingPromoterClickedUrls() {
        return apiPromotedTrack == null ? Collections.<String>emptyList() : apiPromotedTrack.getTrackingPromoterClickedUrls();
    }

    public List<String> getTrackingTrackClickedUrls() {
        return apiPromotedTrack == null ? Collections.<String>emptyList() : apiPromotedTrack.getTrackingTrackClickedUrls();
    }

    public List<String> getTrackingTrackImpressionUrls() {
        return apiPromotedTrack == null ? Collections.<String>emptyList() : apiPromotedTrack.getTrackingTrackImpressionUrls();
    }

    public List<String> getTrackingTrackPlayedUrls() {
        return apiPromotedTrack == null ? Collections.<String>emptyList() : apiPromotedTrack.getTrackingTrackPlayedUrls();
    }
}
