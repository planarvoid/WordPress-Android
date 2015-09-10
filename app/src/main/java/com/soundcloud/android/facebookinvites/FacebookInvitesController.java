package com.soundcloud.android.facebookinvites;

import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class FacebookInvitesController extends DefaultActivityLightCycle<AppCompatActivity> {

    private final FacebookInvitesStorage facebookInvitesStorage;

    @Inject
    public FacebookInvitesController(FacebookInvitesStorage facebookInvitesStorage) {
        this.facebookInvitesStorage = facebookInvitesStorage;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        facebookInvitesStorage.setAppOpened();
    }

    @Override
    public void onNewIntent(AppCompatActivity activity, Intent intent) {
        facebookInvitesStorage.setAppOpened();
    }

}
