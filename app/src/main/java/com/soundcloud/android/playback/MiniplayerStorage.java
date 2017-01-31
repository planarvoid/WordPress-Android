package com.soundcloud.android.playback;

import com.soundcloud.android.storage.StorageModule;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

public class MiniplayerStorage {

    private static final String MINIMIZED_PLAYER_MANUALLY = "MINIMIZED_PLAYER_MANUALLY";

    private final SharedPreferences preferences;

    @Inject
    public MiniplayerStorage(@Named(StorageModule.PLAYER) SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public void clear() {
        setMinimizedPlayerManually(false);
    }

    public void setMinimizedPlayerManually() {
        setMinimizedPlayerManually(true);
    }

    public boolean hasMinimizedPlayerManually() {
        return preferences.getBoolean(MINIMIZED_PLAYER_MANUALLY, false);
    }

    private void setMinimizedPlayerManually(boolean value) {
        preferences.edit().putBoolean(MINIMIZED_PLAYER_MANUALLY, value).apply();
    }

}
