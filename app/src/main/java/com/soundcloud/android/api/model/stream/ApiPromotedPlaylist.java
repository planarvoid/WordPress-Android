package com.soundcloud.android.api.model.stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;

import java.util.Collections;
import java.util.List;

public class ApiPromotedPlaylist {

    private final ApiPlaylist apiPlaylist;
    private final ApiUser promoter;
    private final String adUrn;
    private final List<String> trackingPlaylistClickedUrls;
    private final List<String> trackingPlaylistImpressionUrls;
    private final List<String> trackingProfileClickedUrls;
    private final List<String> trackingPromoterClickedUrls;

    public ApiPromotedPlaylist(@JsonProperty("playlist") ApiPlaylist apiPlaylist,
                               @JsonProperty("promoter") ApiUser promoter,
                               @JsonProperty("urn") String adUrn,
                               @JsonProperty("tracking_playlist_clicked_urls") List<String> trackingPlaylistClickedUrls,
                               @JsonProperty("tracking_playlist_impression_urls") List<String> trackingPlaylistImpressionUrls,
                               @JsonProperty("tracking_profile_clicked_urls") List<String> trackingProfileClickedUrls,
                               @JsonProperty("tracking_promoter_clicked_urls") List<String> trackingPromoterClickedUrls) {

        this.apiPlaylist = apiPlaylist;
        this.promoter = promoter;
        this.adUrn = adUrn;
        this.trackingPlaylistClickedUrls = trackingPlaylistClickedUrls;
        this.trackingPlaylistImpressionUrls = trackingPlaylistImpressionUrls;
        this.trackingProfileClickedUrls = trackingProfileClickedUrls;
        this.trackingPromoterClickedUrls = trackingPromoterClickedUrls;
    }

    public ApiPlaylist getApiPlaylist() {
        return apiPlaylist;
    }

    public ApiUser getPromoter() {
        return promoter;
    }

    public String getAdUrn() {
        return adUrn;
    }

    public List<String> getTrackingPlaylistClickedUrls() {
        return trackingPlaylistClickedUrls;
    }

    public List<String> getTrackingPlaylistImpressionUrls() {
        return trackingPlaylistImpressionUrls;
    }

    public List<String> getTrackingProfileClickedUrls() {
        return trackingProfileClickedUrls;
    }

    public List<String> getTrackingPromoterClickedUrls() {
        return trackingPromoterClickedUrls;
    }

    public List<String> getTrackingTrackPlayedUrls() {
        // We definitely track plays for tracks from promoted playlists,
        // however we don't get anything from api-mobile for this set of trackers.
        // This may change in the future so we have a place for that.
        return Collections.emptyList();
    }
}
