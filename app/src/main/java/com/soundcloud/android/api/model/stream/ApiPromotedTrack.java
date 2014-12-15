package com.soundcloud.android.api.model.stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;

import java.util.List;

public class ApiPromotedTrack {

    private final ApiTrack apiTrack;
    private final ApiUser promoter;
    private final Urn urn;
    private final List<String> trackingTrackClickedUrls;
    private final List<String> trackingProfileClickedUrls;
    private final List<String> trackingPromoterClickedUrls;
    private final List<String> trackingTrackPlayedUrls;
    private final List<String> trackingTrackImpressionUrls;

    public ApiPromotedTrack(@JsonProperty("track") ApiTrack apiTrack,
                            @JsonProperty("promoter") ApiUser promoter,
                            @JsonProperty("urn") Urn urn,
                            @JsonProperty("tracking_track_clicked_urls") List<String> trackingTrackClickedUrls,
                            @JsonProperty("tracking_profile_clicked_urls") List<String> trackingProfileClickedUrls,
                            @JsonProperty("tracking_promoter_clicked_urls") List<String> trackingPromoterClickedUrls,
                            @JsonProperty("tracking_track_played_urls") List<String> trackingTrackPlayedUrls,
                            @JsonProperty("tracking_track_impression_urls") List<String> trackingTrackImpressionUrls) {
        this.apiTrack = apiTrack;
        this.promoter = promoter;
        this.urn = urn;
        this.trackingTrackClickedUrls = trackingTrackClickedUrls;
        this.trackingProfileClickedUrls = trackingProfileClickedUrls;
        this.trackingPromoterClickedUrls = trackingPromoterClickedUrls;
        this.trackingTrackPlayedUrls = trackingTrackPlayedUrls;
        this.trackingTrackImpressionUrls = trackingTrackImpressionUrls;
    }

    public ApiUser getPromoter() {
        return promoter;
    }

    public Urn getUrn() {
        return urn;
    }

    public ApiTrack getApiTrack() {
        return apiTrack;
    }

    public List<String> getTrackingTrackPlayedUrls() {
        return trackingTrackPlayedUrls;
    }

    public List<String> getTrackingTrackClickedUrls() {
        return trackingTrackClickedUrls;
    }

    public List<String> getTrackingTrackImpressionUrls() {
        return trackingTrackImpressionUrls;
    }


    public List<String> getTrackingProfileClickedUrls() {
        return trackingProfileClickedUrls;
    }

    public List<String> getTrackingPromoterClickedUrls() {
        return trackingPromoterClickedUrls;
    }
}
