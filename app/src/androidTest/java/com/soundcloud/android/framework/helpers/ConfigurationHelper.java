package com.soundcloud.android.framework.helpers;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigurationHelper {

    private final static String OFFLINE_CONTENT_FEATURE = "offline_sync";

    public static void enableOfflineContent(Context context) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences("features_settings", Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(OFFLINE_CONTENT_FEATURE, true).apply();

        sharedPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(key.equals(OFFLINE_CONTENT_FEATURE) && sharedPreferences.getBoolean(OFFLINE_CONTENT_FEATURE, false)){
                    sharedPreferences.edit().putBoolean(OFFLINE_CONTENT_FEATURE, true).apply();
                }
            }
        });
    }

    public static void disableOfflineSync(Context context) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences("features_settings", Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean("offline_likes", false).putBoolean(OFFLINE_CONTENT_FEATURE, false).apply();
    }
}
