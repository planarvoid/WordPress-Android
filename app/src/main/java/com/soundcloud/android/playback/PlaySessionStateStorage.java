package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.java.strings.Strings;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

public class PlaySessionStateStorage {

    private final SharedPreferences sharedPreferences;

    enum Keys {
        PROGRESS, ITEM
    }

    @Inject
    public PlaySessionStateStorage(@Named(StorageModule.PLAY_SESSION_STATE) SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    void savePlayInfo(Urn currentUrn) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Keys.ITEM.name(), currentUrn.toString());
        editor.apply();
    }

    void saveProgress(long progress) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(Keys.PROGRESS.name(), progress);
        editor.apply();
    }

    long getLastStoredProgress() {
        return sharedPreferences.getLong(Keys.PROGRESS.name(), 0);
    }

    Urn getLastPlayingItem() {
        return sharedPreferences.contains(Keys.ITEM.name()) ?
               new Urn(sharedPreferences.getString(Keys.ITEM.name(), Strings.EMPTY)) : Urn.NOT_SET;
    }
}
