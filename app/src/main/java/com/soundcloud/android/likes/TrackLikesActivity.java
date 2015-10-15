package com.soundcloud.android.likes;

import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;

import javax.inject.Inject;

public class TrackLikesActivity extends ScActivity {

    @Inject @LightCycle PlayerController playerController;

    @Inject BaseLayoutHelper baseLayoutHelper;

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayout(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle(this.getString(R.string.track_likes_title));

        if (savedInstanceState == null) {
            attachFragment();
        }
    }

    private void attachFragment() {
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new TrackLikesFragment()).commit();
    }
}
