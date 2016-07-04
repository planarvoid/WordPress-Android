package com.soundcloud.android.gcm;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.storage.StorageModule;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

public class GcmStorage {

    private final SharedPreferences sharedPreferences;
    private final FeatureFlags featureFlags;

    private static final String TOKEN_KEY = "gcmToken";
    private static final String HAS_REGISTERED_KEY = "hasRegistered";

    @Inject
    public GcmStorage(@Named(StorageModule.GCM) SharedPreferences sharedPreferences,
                      FeatureFlags featureFlags) {
        this.sharedPreferences = sharedPreferences;
        this.featureFlags = featureFlags;
    }

    public boolean shouldRegister(){
        return featureFlags.isEnabled(Flag.ARCHER_PUSH) ? !hasRegistered() : !hasToken();
    }

    private boolean hasRegistered() {
        return sharedPreferences.getBoolean(HAS_REGISTERED_KEY, false);
    }

    public void markAsRegistered(String token) {
        storeToken(token);
        if (featureFlags.isEnabled(Flag.ARCHER_PUSH)) {
            setAsRegistered(true);
        }
    }

    public void clearHasRegistered() {
        clearToken();
        if (featureFlags.isEnabled(Flag.ARCHER_PUSH)) {
            setAsRegistered(false);
        }
    }

    private void setAsRegistered(boolean hasRegistered) {
        sharedPreferences.edit().putBoolean(HAS_REGISTERED_KEY, hasRegistered).apply();
    }

    @Deprecated
    public String getToken() {
        return sharedPreferences.getString(TOKEN_KEY, null);
    }

    @Deprecated // do not rely on tokens anymore, as we will not store them when Archer is in service
    private boolean hasToken() {
        return sharedPreferences.contains(TOKEN_KEY);
    }

    private void storeToken(String token) {
        sharedPreferences.edit().putString(TOKEN_KEY, token).apply();
    }

    private void clearToken() {
        sharedPreferences.edit().remove(TOKEN_KEY).apply();
    }
}
