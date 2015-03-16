package com.soundcloud.android.offline;

import com.soundcloud.android.rx.PreferenceChangeOnSubscribe;
import rx.Observable;
import rx.functions.Func1;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

public class OfflineSettingsStorage {

    private static final String OFFLINE_LIKES_ENABLED = "offline_likes";
    private static final String OFFLINE_WIFI_ONLY = "offline_wifi_only";

    private final SharedPreferences sharedPreferences;

    private static final Func1<String, Boolean> FILTER_OFFLINE_LIKES_KEY = new Func1<String, Boolean>() {
        @Override
        public Boolean call(String key) {
            return OFFLINE_LIKES_ENABLED.equals(key);
        }
    };

    private final Func1<String, Boolean> toValue = new Func1<String, Boolean>() {
        @Override
        public Boolean call(String key) {
            return sharedPreferences.getBoolean(key, false);
        }
    };

    @Inject
    public OfflineSettingsStorage(@Named("OfflineSettings") SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public boolean isOfflineLikedTracksEnabled() {
        return sharedPreferences.getBoolean(OFFLINE_LIKES_ENABLED, false);
    }

    public void setOfflineLikedTracksEnabled(final boolean enabled) {
        sharedPreferences.edit().putBoolean(OFFLINE_LIKES_ENABLED, enabled).apply();
    }

    public boolean isWifiOnlyEnabled() {
        return sharedPreferences.getBoolean(OFFLINE_WIFI_ONLY, true);
    }

    public void setWifiOnlyEnabled(boolean wifiOnly) {
        sharedPreferences.edit().putBoolean(OFFLINE_WIFI_ONLY, wifiOnly).apply();
    }

    public Observable<Boolean> getOfflineLikedTracksChanged() {
        return Observable.create(new PreferenceChangeOnSubscribe(sharedPreferences))
                .filter(FILTER_OFFLINE_LIKES_KEY)
                .map(toValue);
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

}
