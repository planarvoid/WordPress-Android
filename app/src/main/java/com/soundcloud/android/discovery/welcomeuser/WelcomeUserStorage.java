package com.soundcloud.android.discovery.welcomeuser;

import android.content.SharedPreferences;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class WelcomeUserStorage {

    static final String TIMESTAMP_WELCOME_CARD = "TIMESTAMP_WELCOME_CARD";
    private static final int DELTA_HOURS = 12;

    private final SharedPreferences preferences;

    @Inject
    WelcomeUserStorage(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    void onWelcomeUser() {
        preferences.edit().putLong(TIMESTAMP_WELCOME_CARD, new Date().getTime()).apply();
    }

    public boolean shouldShowWelcome() {
        long lastShownTimestamp = preferences.getLong(TIMESTAMP_WELCOME_CARD, 0);
        long timeDeltaFromLastShow = new Date().getTime() - lastShownTimestamp;
        return TimeUnit.HOURS.convert(timeDeltaFromLastShow, TimeUnit.MILLISECONDS) > DELTA_HOURS;
    }
}
