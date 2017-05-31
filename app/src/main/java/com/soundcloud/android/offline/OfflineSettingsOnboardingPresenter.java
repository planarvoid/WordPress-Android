package com.soundcloud.android.offline;

import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.R;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class OfflineSettingsOnboardingPresenter extends DefaultActivityLightCycle<AppCompatActivity> {

    private final NavigationExecutor navigationExecutor;
    private final OfflineSettingsStorage storage;

    private AppCompatActivity activity;

    @Inject
    OfflineSettingsOnboardingPresenter(NavigationExecutor navigationExecutor, OfflineSettingsStorage storage) {
        this.navigationExecutor = navigationExecutor;
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
        navigationExecutor.openOfflineSettings(activity);
    }
}
