package com.soundcloud.android.offline;

import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class OfflineSettingsOnboardingPresenter extends DefaultActivityLightCycle<AppCompatActivity> {

    private final Navigator navigator;
    private final OfflineSettingsStorage storage;

    private AppCompatActivity activity;

    @Inject
    OfflineSettingsOnboardingPresenter(Navigator navigator, OfflineSettingsStorage storage) {
        this.navigator = navigator;
        this.storage = storage;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.activity = activity;
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        this.activity = null;
    }

    void onContinue() {
        storage.setOfflineSettingsOnboardingSeen();
        navigator.navigateTo(NavigationTarget.forOfflineSettings(false));
    }
}
