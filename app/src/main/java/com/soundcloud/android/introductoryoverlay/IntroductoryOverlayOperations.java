package com.soundcloud.android.introductoryoverlay;

import com.soundcloud.android.storage.StorageModule;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

public class IntroductoryOverlayOperations {

    private final SharedPreferences sharedPreferences;

    @Inject
    public IntroductoryOverlayOperations(@Named(StorageModule.INTRODUCTORY_OVERLAYS) SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    void setOverlayShown(String key) {
        sharedPreferences.edit()
                         .putBoolean(key, true)
                         .apply();
    }

    public boolean wasOverlayShown(String key) {
        return sharedPreferences.getBoolean(key, false);

    }
}
