package com.soundcloud.android.upgrade;

import com.soundcloud.android.R;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public class GoOnboardingActivity extends ScActivity {

    @Inject @LightCycle
    GoOnboardingPresenter presenter;

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.go_onboarding);
    }

    @Override
    public Screen getScreen() {
        return Screen.OFFLINE_ONBOARDING;
    }

}
