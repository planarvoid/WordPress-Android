package com.soundcloud.android.playback.ui;

import com.soundcloud.android.storage.StorageModule;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

class PlayerPagerOnboardingStorage {

    private static final String NUMBER_OF_ONBOARDING_RUN = "NUMBER_OF_ONBOARDING_RUN";
    private final SharedPreferences preferences;

    @Inject
    PlayerPagerOnboardingStorage(@Named(StorageModule.PLAYER) SharedPreferences preferences) {
        this.preferences = preferences;
    }

    int numberOfOnboardingRun() {
        return preferences.getInt(NUMBER_OF_ONBOARDING_RUN, 0);
    }

    void increaseNumberOfOnboardingRun() {
        preferences.edit().putInt(NUMBER_OF_ONBOARDING_RUN, numberOfOnboardingRun() + 1).apply();
    }
}
