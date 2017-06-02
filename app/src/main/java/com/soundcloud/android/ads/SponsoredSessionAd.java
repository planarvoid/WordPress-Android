package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import android.net.Uri;

import java.util.List;

@AutoValue
public abstract class SponsoredSessionAd extends AdData implements PrestitialAd {

    public static SponsoredSessionAd create(ApiModel apiModel) {
        final MonetizationType monetizationType = MonetizationType.SPONSORED_SESSION;
        return new AutoValue_SponsoredSessionAd(apiModel.adUrn(),
                                                monetizationType,
                                                apiModel.adFreeMinutes(),
                                                apiModel.rewardUrls(),
                                                apiModel.optInCard(),
                                                VideoAd.create(apiModel.video(), 0L, monetizationType));
    }

    @Override
    public Uri clickthroughUrl() {
        return Uri.parse(optInCard().clickthroughUrl());
    }

    public abstract int adFreeLength();
    public abstract List<String> rewardUrls();
    public abstract OptInCard optInCard();
    public abstract VideoAd video();

    @AutoValue
    public abstract static class OptInCard extends ApiBaseAdVisual {
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
    public abstract static class ApiModel {
        @JsonCreator
        public static ApiModel create(@JsonProperty("ad_urn") Urn urn,
                                      @JsonProperty("ad_free_minutes") int adFreeMinutes,
                                      @JsonProperty("reward_urls") List<String> rewardUrls,
                                      @JsonProperty("video") VideoAd.ApiModel video,
                                      @JsonProperty("opt_in_card") OptInCard optInCard) {
            return new AutoValue_SponsoredSessionAd_ApiModel(urn, adFreeMinutes, rewardUrls, video, optInCard);
        }

        public abstract Urn adUrn();
        public abstract int adFreeMinutes();
        public abstract List<String> rewardUrls();
        public abstract VideoAd.ApiModel video();
        public abstract OptInCard optInCard();
    }
}
