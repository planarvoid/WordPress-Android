package com.soundcloud.android.facebookinvites;


import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.DateProvider;

import android.content.SharedPreferences;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;

public class FacebookInvitesStorage {
    public static final String LAST_CLICK = "last_click";
    public static final String TIMES_APP_OPENED = "times_app_opened";
    public static final String LAST_DISMISS = "last_dismiss";
    public static final String TIMES_DISMISSED = "times_dismissed";

    private final DateProvider dateProvider;
    private final SharedPreferences sharedPreferences;

    @Inject
    public FacebookInvitesStorage(@Named(StorageModule.FACEBOOK_INVITES) SharedPreferences sharedPreferences,
                                  DateProvider dateProvider) {
        this.sharedPreferences = sharedPreferences;
        this.dateProvider = dateProvider;
    }

    public void setAppOpened() {
        incrementCounter(TIMES_APP_OPENED);
    }

    public void setClicked() {
        setCurrentTimestamp(LAST_CLICK);
        resetDismissed();
    }

    public void setDismissed() {
        incrementCounter(TIMES_DISMISSED);
        setCurrentTimestamp(LAST_DISMISS);
    }

    public void resetDismissed() {
        setTimestamp(LAST_DISMISS, 0l);
        setCounter(TIMES_DISMISSED, 0);
    }

    public long getMillisSinceLastClick() {
        return millisSince(LAST_CLICK);
    }

    public long getMillisSinceLastDismiss() {
        return millisSince(LAST_DISMISS);
    }

    public int getTimesAppOpened() {
        return getCounter(TIMES_APP_OPENED);
    }

    public int getTimesDismissed() {
        return getCounter(TIMES_DISMISSED);
    }

    @VisibleForTesting
    public void setTimesAppOpened(int count) {
        setCounter(TIMES_APP_OPENED, count);
    }

    @VisibleForTesting
    public void setLastClick(long ts) {
        setTimestamp(LAST_CLICK, ts);
    }

    private int getCounter(String key) {
        return sharedPreferences.getInt(key, 0);
    }

    private void incrementCounter(String key) {
        setCounter(key, getCounter(key) + 1);
    }

    private void setCounter(String key, int count) {
        sharedPreferences.edit()
                .putInt(key, count)
                .apply();
    }

    private long getTimestamp(String key) {
        return sharedPreferences.getLong(key, 0);
    }

    private long millisSince(String key) {
        return dateProvider.getCurrentTime() - getTimestamp(key);
    }

    private void setTimestamp(String key, long ts) {
        sharedPreferences.edit()
                .putLong(key, ts)
                .apply();
    }

    private void setCurrentTimestamp(String key) {
        setTimestamp(key, dateProvider.getCurrentTime());
    }
}
