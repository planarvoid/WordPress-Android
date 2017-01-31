package com.soundcloud.android.ads;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class AppInstallAd extends AdData implements ExpirableAd {
    private State state = State.init();

    public static AppInstallAd create(ApiAppInstallAd apiAppInstallAd) {
        return create(apiAppInstallAd, System.currentTimeMillis());
    }

    public static AppInstallAd create(ApiAppInstallAd apiAppInstallAd, long createdAt) {
        final ApiAdTracking apiAdTracking = apiAppInstallAd.apiAdTracking();
        return new AutoValue_AppInstallAd(apiAppInstallAd.getAdUrn(),
                                          createdAt,
                                          apiAppInstallAd.getExpiryInMins(),
                                          apiAppInstallAd.getName(),
                                          apiAppInstallAd.getCtaButtonText(),
                                          apiAppInstallAd.getClickThroughUrl(),
                                          apiAppInstallAd.getImageUrl(),
                                          apiAppInstallAd.getRating(),
                                          apiAppInstallAd.getRatersCount(),
                                          apiAdTracking.impressionUrls,
                                          apiAdTracking.clickUrls);
    }

    public abstract long getCreatedAt();

    public abstract int getExpiryInMins();

    public abstract String getName();

    public abstract String getCtaButtonText();

    public abstract String getClickThroughUrl();

    public abstract String getImageUrl();

    public abstract float getRating();

    public abstract int getRatersCount();

    public abstract List<String> getImpressionUrls();

    public abstract List<String> getClickUrls();

    Optional<Date> getImageLoadTime() {
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
            return create(Optional.<Date>absent(), false);
        }
    }
}
