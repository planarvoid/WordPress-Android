package com.soundcloud.android.sync;

import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.IOUtils;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

@Singleton
public class SyncConfig {
    public static final long DEFAULT_SYNC_DELAY = TimeUnit.HOURS.toSeconds(1); // interval between syncs
    public static final String AUTHORITY = "com.soundcloud.android.provider.ScContentProvider";

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

    public boolean isSyncWifiOnlyEnabled() {
        return sharedPreferences.getBoolean(PREF_SYNC_WIFI_ONLY, true);
    }

    public boolean shouldSyncCollections() {
        return !isSyncWifiOnlyEnabled() || IOUtils.isWifiConnected(context);
    }

    public boolean shouldSync(String prefKey, long max) {
        long currentTime = dateProvider.getCurrentTime();
        final long lastAction = sharedPreferences.getLong(prefKey, currentTime);
        return (currentTime - lastAction) > max;
    }

    public boolean isSyncingEnabled(Account account) {
        return ContentResolver.getIsSyncable(account, AUTHORITY) > 0;
    }

    public void enableSyncing(Account account) {
        ContentResolver.setIsSyncable(account, AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(account, AUTHORITY, true);
        ContentResolver.addPeriodicSync(account, AUTHORITY, new Bundle(), DEFAULT_SYNC_DELAY);
    }

    public void disableSyncing(Account account) {
        ContentResolver.setSyncAutomatically(account, AUTHORITY, false);
        ContentResolver.removePeriodicSync(account, AUTHORITY, new Bundle());
    }

    public boolean isAutoSyncing(Account account) {
        return ContentResolver.getSyncAutomatically(account, AUTHORITY);
    }

}
