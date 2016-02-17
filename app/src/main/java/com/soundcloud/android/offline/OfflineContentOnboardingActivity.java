package com.soundcloud.android.offline;

import com.soundcloud.android.R;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public class OfflineContentOnboardingActivity extends LoggedInActivity {

    @Inject @LightCycle OfflineContentOnboardingPresenter presenter;

    @Override
    public Screen getScreen() {
        return Screen.OFFLINE_ONBOARDING;
    }

    @Override
    protected void setActivityContentView() {
        setContentView(R.layout.offline_content_onboarding);
    }

}
