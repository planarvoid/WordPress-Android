package com.soundcloud.android.stream;

import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.annotations.VisibleForTesting;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

class UpsellStorage {

    @VisibleForTesting
    static final String UPSELL_DISMISSED = "upsell_dismissed";

    @VisibleForTesting
    static final long UPSELL_REAPPEAR_DELAY_IN_HOURS = 48;

    private final SharedPreferences sharedPreferences;
    private final DateProvider dateProvider;

    @Inject
    public UpsellStorage(@Named(StorageModule.STREAM) SharedPreferences sharedPreferences,
                         CurrentDateProvider dateProvider) {
        this.sharedPreferences = sharedPreferences;
        this.dateProvider = dateProvider;
    }

    void clearData() {
        sharedPreferences.edit().clear().apply();
    }

    void setUpsellDismissed() {
        sharedPreferences.edit()
                         .putLong(UPSELL_DISMISSED, dateProvider.getCurrentTime())
                         .apply();
    }

    boolean canDisplayUpsell() {
        return !wasUpsellDismissed() || canDisplayAgain();
    }

    private boolean wasUpsellDismissed() {
        return sharedPreferences.contains(UPSELL_DISMISSED);
    }

    private boolean canDisplayAgain() {
        long lastDisplayedMillis = dateProvider.getCurrentTime() - sharedPreferences.getLong(UPSELL_DISMISSED,
                                                                                             dateProvider.getCurrentTime());
        return TimeUnit.MILLISECONDS.toHours(lastDisplayedMillis) >= UPSELL_REAPPEAR_DELAY_IN_HOURS;
    }

}
