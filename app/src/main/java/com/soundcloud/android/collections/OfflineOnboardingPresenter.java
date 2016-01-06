package com.soundcloud.android.collections;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class OfflineOnboardingPresenter extends DefaultActivityLightCycle<AppCompatActivity> implements OfflineOnboardingView.Listener {

    private final OfflineOnboardingView onboardingView;
    private final Navigator navigator;
    private final OfflineContentOperations offlineContentOperations;

    private Activity activity;

    @Inject
    public OfflineOnboardingPresenter(OfflineOnboardingView onboardingView, Navigator navigator,
                                      OfflineContentOperations offlineContentOperations) {
        this.onboardingView = onboardingView;
        this.navigator = navigator;
        this.offlineContentOperations = offlineContentOperations;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);
        this.activity = activity;
        onboardingView.setupContentView(activity, this);
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        this.activity = null;
        super.onDestroy(activity);
    }

    @Override
    public void selectiveSync() {
        navigator.openStream(activity, Screen.OFFLINE_ONBOARDING);
        activity.finish();
    }

    @Override
    public void autoSync() {
        offlineContentOperations.enableOfflineCollection();
        navigator.openCollection(activity);
        activity.finish();
    }

}
