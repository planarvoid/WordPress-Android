package com.soundcloud.android.ads;

import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class AppInstallAd extends AdData {

    public static AppInstallAd create(ApiAppInstallAd apiAppInstallAd) {
        final ApiAdTracking apiAdTracking = apiAppInstallAd.apiAdTracking();
        return new AutoValue_AppInstallAd(apiAppInstallAd.getAdUrn(),
                                          System.currentTimeMillis(),
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

    public abstract Long getCreatedAt();

    public abstract int getExpiryInMins();

    public abstract String getName();

    public abstract String getCtaButtonText();

    public abstract String getClickThroughUrl();

    public abstract String getImageUrl();

    public abstract float getRating();

    public abstract int getRatersCount();

    public abstract List<String> getImpressionUrls();

    public abstract List<String> getClickUrls();
}
