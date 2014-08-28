package com.soundcloud.android.associations;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdPlayerController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.view.screen.ScreenPresenter;

import android.content.Intent;
import android.os.Bundle;

import javax.inject.Inject;

public class WhoToFollowActivity extends ScActivity {
    @Inject SlidingPlayerController playerController;
    @Inject AdPlayerController adPlayerController;
    @Inject ScreenPresenter presenter;

    public WhoToFollowActivity() {
        super();
        addLifeCycleComponent(playerController);
        addLifeCycleComponent(adPlayerController);
        presenter.attach(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.side_menu_who_to_follow));
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(getContentHolderViewId(), ScListFragment.newInstance(Content.SUGGESTED_USERS, Screen.WHO_TO_FOLLOW))
                    .commit();
        }
    }

    @Override
    protected void setContentView() {
        presenter.setBaseLayout();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.WHO_TO_FOLLOW.get());
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