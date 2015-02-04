package com.soundcloud.android.framework.helpers;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigurationHelper {

    public static void enableOfflineSync(Context context) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences("features_settings", Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean("offline_sync", true).apply();
    }

    public static void disableOfflineSync(Context context) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences("features_settings", Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean("likes_offline_sync", false).putBoolean("offline_sync", false).apply();
    }
}
