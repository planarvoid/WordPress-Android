package com.soundcloud.android.stream;

import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

class UpsellStorage {
    private static final String UPSELL_DISMISSED = "upsell_dismissed";

    private final SharedPreferences sharedPreferences;
    private final DateProvider dateProvider;

    @Inject
    public UpsellStorage(@Named(StorageModule.STREAM) SharedPreferences sharedPreferences, CurrentDateProvider dateProvider) {
        this.sharedPreferences = sharedPreferences;
        this.dateProvider = dateProvider;
    }

    public void clearData() {
        sharedPreferences.edit().clear().apply();
    }

    public boolean wasUpsellDismissed() {
        return sharedPreferences.contains(UPSELL_DISMISSED);
    }

    public void setUpsellDismissed() {
        sharedPreferences.edit()
                .putLong(UPSELL_DISMISSED, dateProvider.getCurrentTime())
                .apply();
    }
}
