package com.soundcloud.android.upsell;

import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.annotations.VisibleForTesting;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

class InlineUpsellStorage {

    private static final String UPSELL_DISMISSED = "upsell_dismissed:";
    private static final long UPSELL_REAPPEAR_DELAY_IN_HOURS = 48;

    private final SharedPreferences sharedPreferences;
    private final DateProvider dateProvider;

    @Inject
    public InlineUpsellStorage(@Named(StorageModule.UPSELL) SharedPreferences sharedPreferences,
                               CurrentDateProvider dateProvider) {
        this.sharedPreferences = sharedPreferences;
        this.dateProvider = dateProvider;
    }

    @VisibleForTesting
    static String upsellIdToPrefId(String id) {
        return UPSELL_DISMISSED + id;
    }

    void clearData() {
        sharedPreferences.edit().clear().apply();
    }

    void setUpsellDismissed(String id) {
        sharedPreferences.edit()
                         .putLong(UPSELL_DISMISSED + id, dateProvider.getCurrentTime())
                         .apply();
    }

    boolean canDisplayUpsell(String id) {
        return !wasUpsellDismissed(id) || canDisplayAgain(id);
    }

    private boolean wasUpsellDismissed(String id) {
        return sharedPreferences.contains(UPSELL_DISMISSED + id);
    }

    private boolean canDisplayAgain(String id) {
        long lastDisplayedMillis = dateProvider.getCurrentTime() - sharedPreferences.getLong(UPSELL_DISMISSED + id,
                                                                                             dateProvider.getCurrentTime());
        return TimeUnit.MILLISECONDS.toHours(lastDisplayedMillis) >= UPSELL_REAPPEAR_DELAY_IN_HOURS;
    }

}
