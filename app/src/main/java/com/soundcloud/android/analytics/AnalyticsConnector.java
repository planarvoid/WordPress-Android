package com.soundcloud.android.analytics;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.appboy.AppboyPlaySessionState;
import com.soundcloud.android.analytics.appboy.AppboyWrapper;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

/**
 * Used to deal with tracking library specific screen connection logic, such as binding
 * tracking sessions to screens, without exposing those libraries directly. Being a
 * light-cycle, it can simply be injected into an Activity without the need to call
 * any methods on it directly.
 */
public class AnalyticsConnector extends DefaultActivityLightCycle<AppCompatActivity> {

    private final AppboyWrapper appboy;
    private final AppboyPlaySessionState appboyPlaySessionState;
    private final AccountOperations accountOperations;

    @Inject
    public AnalyticsConnector(AppboyWrapper appboy, AppboyPlaySessionState appboyPlaySessionState,
                              AccountOperations accountOperations) {
        this.appboy = appboy;
        this.appboyPlaySessionState = appboyPlaySessionState;
        this.accountOperations = accountOperations;
    }

    @Override
    public void onStart(AppCompatActivity activity) {
        if (appboy.openSession(activity)) {
            appboy.requestInAppMessageRefresh();
            appboyPlaySessionState.resetSessionPlayed();
        }
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        if (!accountOperations.isCrawler()) {
            appboy.registerInAppMessageManager(activity);
        }
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        appboy.unregisterInAppMessageManager(activity);
    }

    @Override
    public void onStop(AppCompatActivity activity) {
        appboy.closeSession(activity);
    }
}
