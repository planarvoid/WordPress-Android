package com.soundcloud.android.discovery.welcomeuser;

import android.content.SharedPreferences;

import javax.inject.Inject;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class WelcomeUserStorage {

    static final String TIMESTAMP_WELCOME_CARD = "TIMESTAMP_WELCOME_CARD";
    private static final int DELTA = 12;

    private final SharedPreferences preferences;

    @Inject
    public WelcomeUserStorage(SharedPreferences preferences) {
        this.preferences = preferences;
    }


    public void onWelcomeUser() {
        preferences.edit().putLong(TIMESTAMP_WELCOME_CARD, new Date().getTime()).apply();
    }

    public boolean shouldShowWelcome() {
        long lastShownTimestamp = preferences.getLong(TIMESTAMP_WELCOME_CARD, 0);
        long timeDeltaFromLastShow = new Date().getTime() - lastShownTimestamp;
        return TimeUnit.HOURS.convert(timeDeltaFromLastShow, TimeUnit.MILLISECONDS) > DELTA;
    }
}
