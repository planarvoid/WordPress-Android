package com.soundcloud.android.activities;

import com.soundcloud.android.Actions;
import com.soundcloud.android.ads.AdPlayerController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.view.screen.ScreenPresenter;

import android.content.Intent;
import android.os.Bundle;

import javax.inject.Inject;

public class ActivitiesActivity extends ScActivity {
    @Inject SlidingPlayerController playerController;
    @Inject AdPlayerController adPlayerController;
    @Inject ScreenPresenter presenter;

    public ActivitiesActivity() {
        addLifeCycleComponent(playerController);
        addLifeCycleComponent(adPlayerController);
        presenter.attach(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(getContentHolderViewId(), ScListFragment.newInstance(Content.ME_ACTIVITIES, Screen.ACTIVITIES)).commit();
        }
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
