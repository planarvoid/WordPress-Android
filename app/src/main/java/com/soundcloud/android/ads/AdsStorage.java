package com.soundcloud.android.ads;

import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.CurrentDateProvider;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

public class AdsStorage {
    private static final String LAST_PRESTITIAL_FETCH = "last_prestitial_fetch";
    private static final long RELEASE_FETCH_INTERVAL = TimeUnit.MINUTES.toMillis(30);
    private static final long BETA_AND_BELOW_FETCH_INTERVAL = TimeUnit.MINUTES.toMillis(1);

    private final SharedPreferences sharedPreferences;
    private final CurrentDateProvider dateProvider;

    @Inject
    public AdsStorage(@Named(StorageModule.ADS) SharedPreferences sharedPreferences,
                      CurrentDateProvider dateProvider) {
        this.sharedPreferences = sharedPreferences;
        this.dateProvider = dateProvider;
    }

    public boolean shouldShowPrestitial() {
        final long lastTime = sharedPreferences.getLong(LAST_PRESTITIAL_FETCH, 1L);
        return (dateProvider.getCurrentTime() > (lastTime + getFetchInterval()));
    }

    public void setLastPrestitialFetch() {
        sharedPreferences.edit().putLong(LAST_PRESTITIAL_FETCH, dateProvider.getCurrentTime()).apply();
    }

    private long getFetchInterval() {
        return ApplicationProperties.isBetaOrBelow() ? BETA_AND_BELOW_FETCH_INTERVAL : RELEASE_FETCH_INTERVAL;
    }
}