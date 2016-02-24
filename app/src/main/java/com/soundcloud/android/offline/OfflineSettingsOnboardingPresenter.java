package com.soundcloud.android.offline;

import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
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
        ButterKnife.bind(this, activity);
        this.activity = activity;
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        this.activity = null;
    }

    @OnClick(R.id.btn_continue)
    void onContinue() {
        storage.setOfflineSettingsOnboardingSeen();
        navigator.openOfflineSettings(activity);
    }
}
