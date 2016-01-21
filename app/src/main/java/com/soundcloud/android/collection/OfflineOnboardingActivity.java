package com.soundcloud.android.collection;

import com.soundcloud.android.R;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public class OfflineOnboardingActivity extends ScActivity {

    @Inject @LightCycle OfflineOnboardingPresenter presenter;

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.offline_onboarding);
    }

    @Override
    public Screen getScreen() {
        return Screen.OFFLINE_ONBOARDING;
    }

}
