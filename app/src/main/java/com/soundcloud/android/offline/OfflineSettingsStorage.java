package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineContentLocation.DEVICE_STORAGE;
import static com.soundcloud.android.offline.OfflineContentLocation.SD_CARD;

import com.soundcloud.android.rx.PreferenceChangeOnSubscribe;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.IOUtils;
import rx.Observable;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

public class OfflineSettingsStorage {

    public static final long UNLIMITED = Long.MAX_VALUE;
    public static final String OFFLINE_SETTINGS_ONBOARDING = "offline_settings_onboarding";

    private static final String OFFLINE_WIFI_ONLY = "offline_wifi_only";
    private static final String OFFLINE_CONTENT_LOCATION = "offline_content_location";
    private static final String OFFLINE_STORAGE_LIMIT = "offline_storage_limit";
    private static final String OFFLINE_SD_AVAILABILITY_REPORTED = "offline_sd_availability_reported";

    private final SharedPreferences sharedPreferences;
    private final Context context;

    @Inject
    public OfflineSettingsStorage(@Named(StorageModule.OFFLINE_SETTINGS) SharedPreferences sharedPreferences, Context context) {
        this.sharedPreferences = sharedPreferences;
        this.context = context;
    }

    public boolean isWifiOnlyEnabled() {
        return sharedPreferences.getBoolean(OFFLINE_WIFI_ONLY, true);
    }

    public void setWifiOnlyEnabled(boolean wifiOnly) {
        sharedPreferences.edit().putBoolean(OFFLINE_WIFI_ONLY, wifiOnly).apply();
    }

    public OfflineContentLocation getOfflineContentLocation() {
        return OfflineContentLocation.fromId(sharedPreferences.getString(OFFLINE_CONTENT_LOCATION, OfflineContentLocation.DEVICE_STORAGE.id));
    }

    void setOfflineContentLocation(OfflineContentLocation offlineContentLocation) {
        sharedPreferences.edit().putString(OFFLINE_CONTENT_LOCATION, offlineContentLocation.id).apply();
    }

    public boolean isOfflineContentAccessible() {
        OfflineContentLocation offlineContentLocation = getOfflineContentLocation();
        return DEVICE_STORAGE == offlineContentLocation ||
                (SD_CARD == offlineContentLocation && IOUtils.isSDCardMounted(context));
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

    public boolean hasSeenOfflineSettingsOnboarding() {
        return sharedPreferences.getBoolean(OFFLINE_SETTINGS_ONBOARDING, false);
    }

    void setOfflineSettingsOnboardingSeen() {
        sharedPreferences.edit().putBoolean(OFFLINE_SETTINGS_ONBOARDING, true).apply();
    }

    boolean hasReportedSdCardAvailability() {
        return sharedPreferences.getBoolean(OFFLINE_SD_AVAILABILITY_REPORTED, false);
    }

    void setSdCardAvailabilityReported() {
        sharedPreferences.edit().putBoolean(OFFLINE_SD_AVAILABILITY_REPORTED, true).apply();
    }

    Observable<Boolean> getWifiOnlyOfflineSyncStateChange() {
        return Observable.create(new PreferenceChangeOnSubscribe(sharedPreferences))
                         .filter(OFFLINE_WIFI_ONLY::equals)
                         .map(key -> sharedPreferences.getBoolean(key, false));
    }

    public Observable<Void> getOfflineContentLocationChange() {
        return Observable.create(new PreferenceChangeOnSubscribe(sharedPreferences))
                         .filter(OFFLINE_CONTENT_LOCATION::equals)
                         .map(RxUtils.TO_VOID);
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}
