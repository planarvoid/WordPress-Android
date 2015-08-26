package com.soundcloud.android.activities;

import com.soundcloud.android.Actions;
import com.soundcloud.android.actionbar.ActionBarHelper;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.lightcycle.LightCycle;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import javax.inject.Inject;

public class ActivitiesActivity extends ScActivity {
    @Inject @LightCycle PlayerController playerController;
    @Inject @LightCycle ActionBarHelper actionBarHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(getContentHolderViewId(), ScListFragment.newInstance(Content.ME_ACTIVITIES, Screen.ACTIVITIES)).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        configureMainOptionMenuItems(menu);
        return true;
    }

    @Override
    protected void setContentView() {
        presenter.setBaseLayoutWithMargins();
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
        startActivity(new Intent(Actions.STREAM).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
        return true;
    }
}
