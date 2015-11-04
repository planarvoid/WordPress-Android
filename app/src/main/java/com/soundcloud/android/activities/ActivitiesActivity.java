package com.soundcloud.android.activities;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.actionbar.ActionBarHelper;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

public class ActivitiesActivity extends ScActivity {
    @Inject @LightCycle PlayerController playerController;
    @Inject @LightCycle ActionBarHelper actionBarHelper;

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject Navigator navigator;
    @Inject FeatureFlags featureFlags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            final Fragment fragment = featureFlags.isEnabled(Flag.ACTIVITIES_REFACTOR)
                    ? new ActivitiesFragment()
                    : ScListFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .add(getContentHolderViewId(), fragment).commit();
        }
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayoutWithMargins(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.ACTIVITIES));
        }
    }

    @Override
    public void onBackPressed() {
        if (!playerController.handleBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        navigator.openHome(this);
        finish();
        return true;
    }
}
