package com.soundcloud.android.facebookinvites;


import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;

import android.content.SharedPreferences;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;

public class FacebookInvitesStorage {
    private static final String LAST_CLICK = "last_click";
    private static final String TIMES_APP_OPENED = "times_app_opened";
    private static final String LAST_DISMISS = "last_dismiss";
    private static final String CREATORS_LAST_DISMISS = "creators_last_dismiss";
    private static final String TIMES_DISMISSED = "times_dismissed";

    private final DateProvider dateProvider;
    private final SharedPreferences sharedPreferences;

    @Inject
    public FacebookInvitesStorage(@Named(StorageModule.FACEBOOK_INVITES) SharedPreferences sharedPreferences,
                                  CurrentDateProvider dateProvider) {
        this.sharedPreferences = sharedPreferences;
        this.dateProvider = dateProvider;
    }

    @VisibleForTesting
    public void setTimesAppOpened(int count) {
        setCounter(TIMES_APP_OPENED, count);
    }

    @VisibleForTesting
    public void setLastClick(long ts) {
        setTimestamp(LAST_CLICK, ts);
    }

    @VisibleForTesting
    public void setLastCreatorDismissMillisAgo(long ts) {
        setTimestamp(CREATORS_LAST_DISMISS, dateProvider.getCurrentTime() - ts);
    }

    public void resetDismissed() {
        setTimestamp(LAST_DISMISS, 0L);
        setCounter(TIMES_DISMISSED, 0);
    }

    void setAppOpened() {
        incrementCounter(TIMES_APP_OPENED);
    }

    void setClicked() {
        setCurrentTimestamp(LAST_CLICK);
        resetDismissed();
    }

    void setDismissed() {
        incrementCounter(TIMES_DISMISSED);
        setCurrentTimestamp(LAST_DISMISS);
    }

    void setCreatorDismissed() {
        setCurrentTimestamp(CREATORS_LAST_DISMISS);
    }

    long getMillisSinceLastClick() {
        return millisSince(LAST_CLICK);
    }

    long getMillisSinceLastListenerDismiss() {
        return millisSince(LAST_DISMISS);
    }

    long getMillisSinceLastCreatorDismiss() {
        return millisSince(CREATORS_LAST_DISMISS);
    }

    int getTimesAppOpened() {
        return getCounter(TIMES_APP_OPENED);
    }

    int getTimesListenerDismissed() {
        return getCounter(TIMES_DISMISSED);
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
