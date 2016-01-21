package com.soundcloud.android.sync;

import com.soundcloud.android.Consts;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.IOUtils;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class SyncConfig {
    public static final long DEFAULT_SYNC_DELAY = TimeUnit.HOURS.toSeconds(1); // interval between syncs

    static final long DEFAULT_NOTIFICATIONS_FREQUENCY = TimeUnit.HOURS.toMillis(4);

    static final long TRACK_STALE_TIME      = TimeUnit.HOURS.toMillis(1);
    static final long ACTIVITY_STALE_TIME   = TimeUnit.HOURS.toMillis(6);
    static final long USER_STALE_TIME       = TimeUnit.HOURS.toMillis(12);
    static final long PLAYLIST_STALE_TIME   = TimeUnit.HOURS.toMillis(6);

    static int[] DEFAULT_BACKOFF_MULTIPLIERS = new int[]{1, 2, 4, 8, 12, 18, 24};
    static int[] USER_BACKOFF_MULTIPLIERS = new int[]{1, 2, 3};

    private static final String PREF_SYNC_WIFI_ONLY = "syncWifiOnly";

    private final SharedPreferences sharedPreferences;
    private final CurrentDateProvider dateProvider;
    private final Context context;

    @Inject
    public SyncConfig(SharedPreferences sharedPreferences, CurrentDateProvider dateProvider, Context context) {
        this.sharedPreferences = sharedPreferences;
        this.dateProvider = dateProvider;
        this.context = context;
    }

    public boolean isNotificationsWifiOnlyEnabled() {
        return sharedPreferences.getBoolean(Consts.PrefKeys.NOTIFICATIONS_WIFI_ONLY, false);
    }

    public boolean isIncomingEnabled() {
        return sharedPreferences.getBoolean(Consts.PrefKeys.NOTIFICATIONS_INCOMING, true);
    }

    public boolean isLikeNotificationEnabled() {
        return sharedPreferences.getBoolean(Consts.PrefKeys.NOTIFICATIONS_LIKES, true);
    }

    public boolean isRepostNotificationsEnabled() {
        return sharedPreferences.getBoolean(Consts.PrefKeys.NOTIFICATIONS_REPOSTS, true);
    }

    public boolean isServerSideNotifications() {
        return sharedPreferences.getBoolean(Consts.PrefKeys.NOTIFICATIONS_SERVER_SIDE, false);
    }

    public boolean isNewFollowerNotificationsEnabled() {
        return sharedPreferences.getBoolean(Consts.PrefKeys.NOTIFICATIONS_FOLLOWERS, true);
    }

    public boolean isActivitySyncEnabled() {
        return isLikeNotificationEnabled() || isCommentNotificationsEnabled();
    }

    public boolean isCommentNotificationsEnabled() {
        return sharedPreferences.getBoolean(Consts.PrefKeys.NOTIFICATIONS_COMMENTS, true);
    }

    public boolean isSyncWifiOnlyEnabled() {
        return sharedPreferences.getBoolean(PREF_SYNC_WIFI_ONLY, true);
    }

    public long getNotificationsFrequency() {
        if (sharedPreferences.contains(Consts.PrefKeys.NOTIFICATIONS_FREQUENCY)) {
            return Long.parseLong(sharedPreferences.getString(Consts.PrefKeys.NOTIFICATIONS_FREQUENCY,
                    String.valueOf(DEFAULT_NOTIFICATIONS_FREQUENCY)));
        } else {
            return DEFAULT_NOTIFICATIONS_FREQUENCY;
        }
    }

    public boolean shouldUpdateDashboard() {
        return !isNotificationsWifiOnlyEnabled() || IOUtils.isWifiConnected(context);
    }

    public boolean shouldSyncCollections() {
        return !isSyncWifiOnlyEnabled() || IOUtils.isWifiConnected(context);
    }

    public boolean shouldSync(String prefKey, long max) {
        long currentTime = dateProvider.getCurrentTime();
        final long lastAction = sharedPreferences.getLong(prefKey, currentTime);
        return (currentTime - lastAction) > max;
    }

    public void enableServerSideNotifications() {
        setServerSideNotifications(true);
    }

    public void disableServerSideNotifications() {
        setServerSideNotifications(false);
    }

    private void setServerSideNotifications(boolean isServerSideNotifications) {
        sharedPreferences.edit()
                .putBoolean(Consts.PrefKeys.NOTIFICATIONS_SERVER_SIDE, isServerSideNotifications)
                .apply();
    }
}
