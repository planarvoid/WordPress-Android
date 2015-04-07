package com.soundcloud.android.framework.helpers;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigurationHelper {

    private final static String OFFLINE_CONTENT = "offline_sync";
    private final static String OFFLINE_UPSELL = "offline_sync_upsell";

    public static void enableOfflineContent(Context context) {
        enableFeature(context, OFFLINE_CONTENT);
    }

    public static void enableUpsell(Context context) {
        enableFeature(context, OFFLINE_UPSELL);
    }

    private static void enableFeature(Context context, final String feature) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences("features_settings", Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(feature, true).apply();

        sharedPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(feature) && sharedPreferences.getBoolean(feature, false)) {
                    sharedPreferences.edit().putBoolean(feature, true).apply();
                }
            }
        });
    }

    public static void disableOfflineSync(Context context) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences("features_settings", Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean("offline_likes", false).putBoolean(OFFLINE_CONTENT, false).apply();
    }

}
