package com.soundcloud.android.downgrade;

import com.soundcloud.android.R;
import com.soundcloud.android.main.EnterScreenDispatcher;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import javax.inject.Inject;

public class GoOffboardingActivity extends RootActivity implements EnterScreenDispatcher.Listener {
    @Inject @LightCycle EnterScreenDispatcher enterScreenDispatcher;
    private GoOffboardingFragment fragment;

    @Override
    public void onEnterScreen(RootActivity activity) {
        fragment.enterScreen();
    }

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.go_offboarding_activity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final FragmentManager fm = getSupportFragmentManager();
        fragment = (GoOffboardingFragment) fm.findFragmentById(R.id.go_offboarding_fragment);
        if (fragment == null) {
            fragment = new GoOffboardingFragment();
            fm.beginTransaction()
              .replace(R.id.go_offboarding_fragment, fragment)
              .commit();
        }

        enterScreenDispatcher.setListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        fragment = null;
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
