package com.soundcloud.android.sync;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.utils.CurrentDateProvider;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.os.Bundle;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

@Singleton
public class SyncConfig {
    private static final long DEFAULT_SYNC_DELAY = TimeUnit.HOURS.toSeconds(1); // interval between syncs
    public static final String AUTHORITY = BuildConfig.ACCOUNT_AUTHORITY;

    private final SharedPreferences sharedPreferences;
    private final CurrentDateProvider dateProvider;

    @Inject
    public SyncConfig(SharedPreferences sharedPreferences,
                      CurrentDateProvider dateProvider) {
        this.sharedPreferences = sharedPreferences;
        this.dateProvider = dateProvider;
    }

    boolean shouldSync(String prefKey, long max) {
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
}
