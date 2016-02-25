package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.offline.OfflineSettingsOnboardingActivity;

public class OfflineSettingsOnboardingScreen extends Screen {

    private static final Class ACTIVITY = OfflineSettingsOnboardingActivity.class;
    private static final int CONTINUE_BUTTON = R.id.btn_continue;

    public OfflineSettingsOnboardingScreen(Han solo) {
        super(solo);
    }

    public OfflineSettingsScreen clickContinue() {
        testDriver.findOnScreenElement(With.id(CONTINUE_BUTTON)).click();
        return new OfflineSettingsScreen(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
