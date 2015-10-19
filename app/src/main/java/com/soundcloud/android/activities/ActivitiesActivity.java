package com.soundcloud.android.activities;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.actionbar.ActionBarHelper;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;

import javax.inject.Inject;

public class ActivitiesActivity extends ScActivity {
    @Inject @LightCycle PlayerController playerController;
    @Inject @LightCycle ActionBarHelper actionBarHelper;

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject Navigator navigator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(getContentHolderViewId(), ScListFragment.newInstance()).commit();
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
