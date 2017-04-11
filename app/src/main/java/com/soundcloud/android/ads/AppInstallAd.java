package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class AppInstallAd extends AdData implements ExpirableAd {
    private State state = State.init();

    public static AppInstallAd create(ApiModel apiModel) {
        return create(apiModel, System.currentTimeMillis());
    }

    public static AppInstallAd create(ApiModel apiModel, long createdAt) {
        final ApiAdTracking adTracking = apiModel.adTracking();
        return new AutoValue_AppInstallAd(apiModel.adUrn(),
                                          MonetizationType.INLAY,
                                          createdAt,
                                          apiModel.expiryInMins(),
                                          apiModel.name(),
                                          apiModel.ctaButtonText(),
                                          apiModel.clickThroughUrl(),
                                          apiModel.imageUrl(),
                                          apiModel.rating(),
                                          apiModel.ratersCount(),
                                          adTracking.impressionUrls,
                                          adTracking.clickUrls);
    }

    public abstract long createdAt();

    public abstract int expiryInMins();

    public abstract String name();

    public abstract String ctaButtonText();

    public abstract String clickThroughUrl();

    public abstract String imageUrl();

    public abstract float rating();

    public abstract int ratersCount();

    public abstract List<String> impressionUrls();

    public abstract List<String> clickUrls();

    Optional<Date> imageLoadTime() {
        return state.getImageLoadTime();
    }

    void setImageLoadTimeOnce(Date date) {
        state = state.withImageLoadTime(date);
    }

    boolean hasReportedImpression() {
        return state.hasReportedImpression();
    }

    void setImpressionReported() {
        state = state.withImpressionReported();
    }

    @AutoValue
    static abstract class State {
        public abstract Optional<Date> getImageLoadTime();
        public abstract boolean hasReportedImpression();

        State withImageLoadTime(Date date) {
            return getImageLoadTime().isPresent() ? this : create(Optional.of(date), hasReportedImpression());
        }

        State withImpressionReported() {
            return hasReportedImpression() ? this : create(getImageLoadTime(), true);
        }

        static State create(Optional<Date> imageLoadTime, boolean hasReportedImpression) {
            return new AutoValue_AppInstallAd_State(imageLoadTime, hasReportedImpression);
        }

        static State init() {
            return create(Optional.absent(), false);
        }
    }

    @AutoValue
    abstract static class ApiModel {
        @JsonCreator
        public static ApiModel create(@JsonProperty("urn") Urn adUrn,
                                      @JsonProperty("expiry_in_minutes") int expiryInMins,
                                      @JsonProperty("name") String name,
                                      @JsonProperty("cta_button_text") String ctaButtonText,
                                      @JsonProperty("clickthrough_url") String clickthroughUrl,
                                      @JsonProperty("image_url") String imageUrl,
                                      @JsonProperty("rating") float rating,
                                      @JsonProperty("rater_count") int ratersCount,
                                      @JsonProperty("app_install_tracking") ApiAdTracking tracking) {
            return new AutoValue_AppInstallAd_ApiModel(adUrn, expiryInMins, name, ctaButtonText, clickthroughUrl,
                                                       imageUrl, rating, ratersCount, tracking);
        }

        public abstract Urn adUrn();

        public abstract int expiryInMins();

        public abstract String name();

        public abstract String ctaButtonText();

        public abstract String clickThroughUrl();

        public abstract String imageUrl();

        public abstract float rating();

        public abstract int ratersCount();

        public abstract ApiAdTracking adTracking();
    }
}
