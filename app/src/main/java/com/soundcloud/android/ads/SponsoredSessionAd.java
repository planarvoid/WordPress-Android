package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.List;

@AutoValue
public abstract class SponsoredSessionAd extends AdData {

    public static SponsoredSessionAd create(ApiModel apiModel) {
        final MonetizationType monetizationType = MonetizationType.SPONSORED_SESSION;
        return new AutoValue_SponsoredSessionAd(apiModel.adUrn(),
                                                monetizationType,
                                                apiModel.adFreeMinutes(),
                                                apiModel.optInCard(),
                                                VideoAd.create(apiModel.video(), 0L, monetizationType));
    }

    public abstract int adFreeLength();
    public abstract OptInCard optInCard();
    public abstract VideoAd video();

    @AutoValue
    abstract static class OptInCard extends ApiBaseAdVisual {
        @JsonCreator
        static OptInCard create(@JsonProperty("urn") Urn adUrn,
                                @JsonProperty("image_url") String imageUrl,
                                @JsonProperty("clickthrough_url") String clickthroughUrl,
                                @JsonProperty("tracking_impression_urls") List<String> trackingImpressionUrls,
                                @JsonProperty("tracking_click_urls") List<String> trackingClickUrls) {
            return new AutoValue_SponsoredSessionAd_OptInCard(adUrn, imageUrl, clickthroughUrl, trackingImpressionUrls, trackingClickUrls);
        }
    }

    @AutoValue
    abstract static class ApiModel {
        @JsonCreator
        public static ApiModel create(@JsonProperty("ad_urn") Urn urn,
                                      @JsonProperty("ad_free_minutes") int adFreeMinutes,
                                      @JsonProperty("video") VideoAd.ApiModel video,
                                      @JsonProperty("opt_in_card") OptInCard optInCard) {
            return new AutoValue_SponsoredSessionAd_ApiModel(urn, adFreeMinutes, video, optInCard);
        }

        public abstract Urn adUrn();
        public abstract int adFreeMinutes();
        public abstract VideoAd.ApiModel video();
        public abstract OptInCard optInCard();
    }
}
