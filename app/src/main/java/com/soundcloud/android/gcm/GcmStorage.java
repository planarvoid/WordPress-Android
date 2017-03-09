package com.soundcloud.android.gcm;

import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.annotations.VisibleForTesting;

import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Named;

public class GcmStorage {

    private final SharedPreferences sharedPreferences;
    private final ApplicationProperties applicationProperties;

    @VisibleForTesting
    static final String TOKEN_KEY = "gcmToken";
    private static final String HAS_REGISTERED_KEY = "hasRegistered";

    @Inject
    public GcmStorage(@Named(StorageModule.GCM) SharedPreferences sharedPreferences,
                      ApplicationProperties applicationProperties) {
        this.sharedPreferences = sharedPreferences;
        this.applicationProperties = applicationProperties;
    }

    public boolean shouldRegister(){
        return applicationProperties.registerForGcm() ? !hasRegistered() : !hasToken();
    }

    private boolean hasRegistered() {
        return sharedPreferences.getBoolean(HAS_REGISTERED_KEY, false);
    }

    public void markAsRegistered(String token) {
        storeToken(token);
        if (applicationProperties.registerForGcm()) {
            setAsRegistered(true);
        }
    }

    public void clearTokenForRefresh() {
        clearToken();
        if (applicationProperties.registerForGcm()) {
            setAsRegistered(false);
        }
    }

    private void setAsRegistered(boolean hasRegistered) {
        sharedPreferences.edit().putBoolean(HAS_REGISTERED_KEY, hasRegistered).apply();
    }

    @Nullable
    public String getToken() {
        return sharedPreferences.getString(TOKEN_KEY, null);
    }

    @Deprecated // do not rely on tokens anymore, as we will not store them when Archer is in service
    private boolean hasToken() {
        return sharedPreferences.contains(TOKEN_KEY);
    }

    @Deprecated
    private void storeToken(String token) {
        sharedPreferences.edit().putString(TOKEN_KEY, token).apply();
    }

    @Deprecated
    private void clearToken() {
        sharedPreferences.edit().remove(TOKEN_KEY).apply();
    }
}
