package com.soundcloud.android.downgrade;

import com.soundcloud.android.R;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;

import javax.inject.Inject;

public class GoOffboardingActivity extends RootActivity {

    @Inject @LightCycle GoOffboardingPresenter presenter;

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.go_offboarding);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (screenTracker.isEnteringScreen()) {
            presenter.trackResubscribeButtonImpression();
        }
    }

    @Override
    public Screen getScreen() {
        return Screen.OFFLINE_OFFBOARDING;
    }

    @Override
    protected boolean receiveConfigurationUpdates() {
        return false;
    }
}
