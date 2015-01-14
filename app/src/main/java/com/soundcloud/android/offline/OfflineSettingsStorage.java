package com.soundcloud.android.offline;

import com.soundcloud.android.rx.PreferenceChangeOnSubscribe;
import rx.Observable;
import rx.functions.Func1;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

public class OfflineSettingsStorage {

    private static final String LIKES_OFFLINE_SYNC_ENABLED = "likes_offline_sync";

    private final SharedPreferences sharedPreferences;

    private static final Func1<String, Boolean> FILTER_OFFLINE_LIKES_KEY = new Func1<String, Boolean>() {
        @Override
        public Boolean call(String key) {
            return LIKES_OFFLINE_SYNC_ENABLED.equals(key);
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

    public boolean isLikesOfflineSyncEnabled() {
        return sharedPreferences.getBoolean(LIKES_OFFLINE_SYNC_ENABLED, false);
    }

    public void setLikesOfflineSync(final boolean enabled) {
        sharedPreferences.edit().putBoolean(LIKES_OFFLINE_SYNC_ENABLED, enabled).apply();
    }

    public Observable<Boolean> getLikesOfflineSyncChanged() {
        return Observable.create(new PreferenceChangeOnSubscribe(sharedPreferences))
                .filter(FILTER_OFFLINE_LIKES_KEY)
                .map(toValue);
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

}
