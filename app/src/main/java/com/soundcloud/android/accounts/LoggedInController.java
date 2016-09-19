package com.soundcloud.android.accounts;

import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class LoggedInController extends DefaultActivityLightCycle<AppCompatActivity> {

    private final AccountOperations accountOperations;
    private final PlaybackServiceController serviceController;

    @Inject
    public LoggedInController(AccountOperations accountOperations, PlaybackServiceController serviceController) {
        this.accountOperations = accountOperations;
        this.serviceController = serviceController;
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        if (!accountOperations.isUserLoggedIn()) {
            serviceController.resetPlaybackService();
            accountOperations.triggerLoginFlow(activity);
            activity.finish();
        }
    }
}
