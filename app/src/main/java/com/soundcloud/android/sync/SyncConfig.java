package com.soundcloud.android.sync;

import com.soundcloud.android.Consts;
import com.soundcloud.android.utils.IOUtils;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;

public final class SyncConfig {
    private static final long DEFAULT_NOTIFICATIONS_FREQUENCY = 60*60*1000*4L; // 4h

    public static final String PREF_SYNC_WIFI_ONLY          = "syncWifiOnly";

    public static final long DEFAULT_ATTEMPT_DELAY  = 30*60*1000;         // 30 mins in ms
    public static final long DEFAULT_STALE_TIME  = 60*60*1000;         // 1 hr in ms

    public static final long TRACK_STALE_TIME           = DEFAULT_STALE_TIME;
    public static final long ACTIVITY_STALE_TIME        = DEFAULT_STALE_TIME * 4;
    public static final long USER_STALE_TIME            = DEFAULT_STALE_TIME * 12;  // users aren't as crucial
    public static final long PLAYLIST_STALE_TIME        = DEFAULT_STALE_TIME * 6;
    public static final long SHORTCUTS_STALE_TIME       = DEFAULT_STALE_TIME * 24;

    public static final long DEFAULT_SYNC_DELAY   = 3600L; // interval between syncs
    public static int[] DEFAULT_BACKOFF_MULTIPLIERS = new int[]{1, 2, 4, 8, 12, 18, 24};
    public static int[] USER_BACKOFF_MULTIPLIERS  = new int[]{1, 2, 3};


    public static boolean isNotificationsWifiOnlyEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean(Consts.PrefKeys.NOTIFICATIONS_WIFI_ONLY, false);
    }

    public static boolean isIncomingEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean(Consts.PrefKeys.NOTIFICATIONS_INCOMING, true);
    }

    public static boolean isLikeNotificationEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean(Consts.PrefKeys.NOTIFICATIONS_LIKES, true);
    }

    public static boolean isRepostNotificationsEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean(Consts.PrefKeys.NOTIFICATIONS_REPOSTS, true);
    }

    public static boolean isNewFollowerNotificationsEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean(Consts.PrefKeys.NOTIFICATIONS_FOLLOWERS, true);
    }

    public static boolean isActivitySyncEnabled(Context c, Bundle extras) {
        return isLikeNotificationEnabled(c) || isCommentNotificationsEnabled(c);
    }

    public static boolean isCommentNotificationsEnabled(Context c) {
        return PreferenceManager
                .getDefaultSharedPreferences(c)
                .getBoolean(Consts.PrefKeys.NOTIFICATIONS_COMMENTS, true);
    }

    public static boolean isSyncWifiOnlyEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean(PREF_SYNC_WIFI_ONLY, true);
    }

    public static long getNotificationsFrequency(Context c) {
        if (PreferenceManager.getDefaultSharedPreferences(c).contains(Consts.PrefKeys.NOTIFICATIONS_FREQUENCY)) {
            return Long.parseLong(PreferenceManager.getDefaultSharedPreferences(c).getString(Consts.PrefKeys.NOTIFICATIONS_FREQUENCY,
                    String.valueOf(DEFAULT_NOTIFICATIONS_FREQUENCY)));
        } else {
            return DEFAULT_NOTIFICATIONS_FREQUENCY;
        }
    }

    public static boolean shouldUpdateDashboard(Context c) {
        return !isNotificationsWifiOnlyEnabled(c) || IOUtils.isWifiConnected(c);
    }

    public static boolean shouldSyncCollections(Context c) {
        return !isSyncWifiOnlyEnabled(c) || IOUtils.isWifiConnected(c);
    }

    public static boolean shouldSync(Context context, String prefKey, long max) {
        final long lastAction = PreferenceManager.getDefaultSharedPreferences(context).getLong(
                prefKey,
                System.currentTimeMillis());

        return (System.currentTimeMillis() - lastAction) > max;
    }

    private SyncConfig() {}
}
