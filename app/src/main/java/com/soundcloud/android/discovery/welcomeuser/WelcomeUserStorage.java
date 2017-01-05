package com.soundcloud.android.discovery.welcomeuser;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import android.content.SharedPreferences;

import javax.inject.Inject;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class WelcomeUserStorage {

    static final String TIMESTAMP_WELCOME_CARD = "TIMESTAMP_WELCOME_CARD";
    private static final int DELTA = 12;

    private final SharedPreferences preferences;
    private final FeatureFlags featureFlags;

    @Inject
    public WelcomeUserStorage(SharedPreferences preferences,
                              FeatureFlags featureFlags) {
        this.preferences = preferences;
        this.featureFlags = featureFlags;
    }


    public void onWelcomeUser() {
        preferences.edit().putLong(TIMESTAMP_WELCOME_CARD, new Date().getTime()).apply();
    }

    public boolean shouldShowWelcome() {
        if (featureFlags.isEnabled(Flag.WELCOME_USER)) {
            return true;
        }

        long lastShownTimestamp = preferences.getLong(TIMESTAMP_WELCOME_CARD, 0);
        long timeDeltaFromLastShow = new Date().getTime() - lastShownTimestamp;
        return TimeUnit.HOURS.convert(timeDeltaFromLastShow, TimeUnit.MILLISECONDS) > DELTA;
    }
}
