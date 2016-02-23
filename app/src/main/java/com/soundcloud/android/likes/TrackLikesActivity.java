package com.soundcloud.android.likes;

import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarHelper;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;

import javax.inject.Inject;

public class TrackLikesActivity extends PlayerActivity {

    @Inject @LightCycle ActionBarHelper actionBarHelper;

    @Inject BaseLayoutHelper baseLayoutHelper;

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayout(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            attachFragment();
        }
    }

    @Override
    public Screen getScreen() {
        return Screen.LIKES;
    }

    private void attachFragment() {
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new TrackLikesFragment()).commit();
    }

}
