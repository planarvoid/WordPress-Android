package com.soundcloud.android.framework.helpers;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigurationHelper {

    private final static String OFFLINE_SYNC_FEATURE = "offline_sync";

    public static void enableOfflineSync(Context context) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences("features_settings", Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(OFFLINE_SYNC_FEATURE, true).apply();

        sharedPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(key.equals(OFFLINE_SYNC_FEATURE) && sharedPreferences.getBoolean(OFFLINE_SYNC_FEATURE, false)){
                    sharedPreferences.edit().putBoolean(OFFLINE_SYNC_FEATURE, true).apply();
                }
            }
        });
    }

    public static void disableOfflineSync(Context context) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences("features_settings", Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean("likes_offline_sync", false).putBoolean(OFFLINE_SYNC_FEATURE, false).apply();
    }
}
