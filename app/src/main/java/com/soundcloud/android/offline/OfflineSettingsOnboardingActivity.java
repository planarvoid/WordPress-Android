package com.soundcloud.android.offline;

import com.soundcloud.android.R;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public class OfflineSettingsOnboardingActivity extends LoggedInActivity {

    @Inject @LightCycle OfflineSettingsOnboardingPresenter presenter;

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.go_onboarding_settings);
    }

    @Override
    public Screen getScreen() {
        return Screen.SETTINGS_AUTOMATIC_SYNC_ONBOARDING;
    }

}
