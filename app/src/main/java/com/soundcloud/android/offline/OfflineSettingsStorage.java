package com.soundcloud.android.offline;

import com.soundcloud.android.rx.PreferenceChangeOnSubscribe;
import com.soundcloud.android.storage.StorageModule;
import rx.Observable;
import rx.functions.Func1;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

public class OfflineSettingsStorage {

    public static final long UNLIMITED = Long.MAX_VALUE;

    private static final String OFFLINE_WIFI_ONLY = "offline_wifi_only";
    private static final String OFFLINE_STORAGE_LIMIT = "offline_storage_limit";
    private static final String LAST_POLICY_UPDATE_CHECK = "last_policy_update_check";
    private static final String OFFLINE_CONTENT = "has_content_offline";

    private final SharedPreferences sharedPreferences;

    private static final Func1<String, Boolean> FILTER_WIFI_ONLY_KEY = new Func1<String, Boolean>() {
        @Override
        public Boolean call(String key) {
            return OFFLINE_WIFI_ONLY.equals(key);
        }
    };

    private final Func1<String, Boolean> toValue = new Func1<String, Boolean>() {
        @Override
        public Boolean call(String key) {
            return sharedPreferences.getBoolean(key, false);
        }
    };

    @Inject
    public OfflineSettingsStorage(@Named(StorageModule.OFFLINE_SETTINGS) SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public boolean isWifiOnlyEnabled() {
        return sharedPreferences.getBoolean(OFFLINE_WIFI_ONLY, true);
    }

    public void setWifiOnlyEnabled(boolean wifiOnly) {
        sharedPreferences.edit().putBoolean(OFFLINE_WIFI_ONLY, wifiOnly).apply();
    }

    public boolean hasStorageLimit() {
        return getStorageLimit() != UNLIMITED;
    }

    public long getStorageLimit() {
        return sharedPreferences.getLong(OFFLINE_STORAGE_LIMIT, UNLIMITED);
    }

    public void setStorageUnlimited() {
        setStorageLimit(UNLIMITED);
    }

    public void setStorageLimit(long limit) {
        sharedPreferences.edit().putLong(OFFLINE_STORAGE_LIMIT, limit).apply();
    }

    public boolean hasAnyOfflineContent() {
        return sharedPreferences.getBoolean(OFFLINE_CONTENT, false);
    }

    public void setHasAnyOfflineContent(boolean hasOfflineContent) {
        sharedPreferences.edit().putBoolean(OFFLINE_CONTENT, hasOfflineContent).apply();
    }

    void setPolicyUpdateCheckTime(long policiesCheckTime) {
        sharedPreferences.edit().putLong(LAST_POLICY_UPDATE_CHECK, policiesCheckTime).apply();
    }

    long getPolicyUpdateCheckTime() {
        return sharedPreferences.getLong(LAST_POLICY_UPDATE_CHECK, 0);
    }

    Observable<Boolean> getWifiOnlyOfflineSyncStateChange() {
        return Observable.create(new PreferenceChangeOnSubscribe(sharedPreferences))
                .filter(FILTER_WIFI_ONLY_KEY)
                .map(toValue);
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

}
