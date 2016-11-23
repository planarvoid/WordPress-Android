package com.soundcloud.android.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;

import javax.inject.Inject;

public class LikedStationsActivity extends PlayerActivity {
    @Inject BaseLayoutHelper baseLayoutHelper;

    public LikedStationsActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

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
        return Screen.LIKED_STATIONS;
    }

    private void attachFragment() {
        LikedStationsFragment fragment = LikedStationsFragment.create();
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

}
