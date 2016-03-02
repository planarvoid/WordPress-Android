package com.soundcloud.android.downgrade;

import com.soundcloud.android.R;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.main.Screen;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

public class GoOffboardingActivity extends RootActivity {

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.go_offboarding_activity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final FragmentManager fm = getSupportFragmentManager();
        GoOffboardingFragment fragment = (GoOffboardingFragment) fm.findFragmentById(R.id.go_offboarding_fragment);
        if (fragment == null) {
            fragment = new GoOffboardingFragment();
            fm.beginTransaction()
                    .replace(R.id.go_offboarding_fragment, fragment)
                    .commit();
        }
        if (screenTracker.isEnteringScreen()) {
            fragment.enterScreen();
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
