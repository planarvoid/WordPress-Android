package com.soundcloud.android.offline;

import com.soundcloud.android.R;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public class OfflineContentOnboardingActivity extends ScActivity {

    @Inject @LightCycle OfflineContentOnboardingPresenter presenter;

    @Override
    public Screen getScreen() {
        // TODO ?
        return Screen.OFFLINE_ONBOARDING;
    }

    @Override
    protected void setActivityContentView() {
        setContentView(R.layout.offline_content_onboarding);
    }

}
