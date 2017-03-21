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
        PROGRESS, DURATION, ITEM, PLAY_ID
    }

    @Inject
    public PlaySessionStateStorage(@Named(StorageModule.PLAY_SESSION_STATE) SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    void savePlayInfo(Urn currentUrn) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Keys.ITEM.name(), currentUrn.toString());
        editor.remove(Keys.PLAY_ID.name());
        editor.apply();
    }

    void savePlayId(String playId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Keys.PLAY_ID.name(), playId);
        editor.apply();
    }

    void clearProgressAndDuration() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(Keys.PROGRESS.name());
        editor.remove(Keys.DURATION.name());
        editor.apply();
    }

    void saveProgress(long progress, long duration) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(Keys.PROGRESS.name(), progress);
        editor.putLong(Keys.DURATION.name(), duration);
        editor.apply();
    }

    long getLastStoredProgress() {
        return sharedPreferences.getLong(Keys.PROGRESS.name(), 0);
    }

    long getLastStoredDuration() {
        return sharedPreferences.getLong(Keys.DURATION.name(), 0);
    }

    Urn getLastPlayingItem() {
        return sharedPreferences.contains(Keys.ITEM.name()) ?
               new Urn(sharedPreferences.getString(Keys.ITEM.name(), Strings.EMPTY)) : Urn.NOT_SET;
    }

    String getLastPlayId() {
        return sharedPreferences.getString(Keys.PLAY_ID.name(), Strings.EMPTY);
    }

    public boolean hasPlayId() {
        return sharedPreferences.contains(Keys.PLAY_ID.name());
    }

    public void clear() {
        clearProgressAndDuration();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(Keys.ITEM.name());
        editor.remove(Keys.PLAY_ID.name());
        editor.apply();
    }
}
