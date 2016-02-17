package com.soundcloud.android.downgrade;

import com.soundcloud.android.R;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public class GoOffboardingActivity extends ScActivity {

    @Inject @LightCycle GoOffboardingPresenter presenter;

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.go_offboarding);
    }

    @Override
    public Screen getScreen() {
        return Screen.OFFLINE_OFFBOARDING;
    }

}
