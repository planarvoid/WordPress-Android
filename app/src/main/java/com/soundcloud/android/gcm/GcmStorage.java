package com.soundcloud.android.gcm;

import com.soundcloud.android.storage.StorageModule;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

public class GcmStorage {

    private final SharedPreferences sharedPreferences;

    private static final String TOKEN_KEY = "gcmToken";

    @Inject
    public GcmStorage(@Named(StorageModule.GCM) SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public boolean hasToken() {
        return sharedPreferences.contains(TOKEN_KEY);
    }

    public String getToken() {
        return sharedPreferences.getString(TOKEN_KEY, null);
    }

    public void storeToken(String token) {
        sharedPreferences.edit().putString(TOKEN_KEY, token).apply();
    }

    public void clearToken() {
        sharedPreferences.edit().remove(TOKEN_KEY).apply();
    }
}
