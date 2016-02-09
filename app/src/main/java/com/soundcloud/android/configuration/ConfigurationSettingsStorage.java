package com.soundcloud.android.configuration;

import static com.soundcloud.android.storage.StorageModule.CONFIGURATION_SETTINGS;

import com.soundcloud.android.Consts;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

class ConfigurationSettingsStorage {
    private static final String LAST_CONFIG_CHECK_TIME = "last_config_check_time";

    private final SharedPreferences sharedPreferences;

    @Inject
    ConfigurationSettingsStorage(@Named(CONFIGURATION_SETTINGS) SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    void setLastConfigurationCheckTime(long timestamp) {
        sharedPreferences.edit().putLong(LAST_CONFIG_CHECK_TIME, timestamp).apply();
    }

    long getLastConfigurationCheckTime() {
        return sharedPreferences.getLong(LAST_CONFIG_CHECK_TIME, Consts.NOT_SET);
    }
}
