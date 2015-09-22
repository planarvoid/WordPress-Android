package com.soundcloud.android.accounts;

import com.soundcloud.android.ServiceInitiator;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class AccountPlaybackController extends DefaultActivityLightCycle<AppCompatActivity> {

    private final AccountOperations accountOperations;
    private final ServiceInitiator serviceInitiator;

    @Inject
    public AccountPlaybackController(AccountOperations accountOperations, ServiceInitiator serviceInitiator) {
        this.accountOperations = accountOperations;
        this.serviceInitiator = serviceInitiator;
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        if (!accountOperations.isUserLoggedIn()) {
            serviceInitiator.resetPlaybackService();
            activity.finish();
        }
    }
}
