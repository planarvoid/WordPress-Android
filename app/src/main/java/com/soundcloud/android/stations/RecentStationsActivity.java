package com.soundcloud.android.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;

import javax.inject.Inject;

public class RecentStationsActivity extends PlayerActivity {
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
        return Screen.STATIONS_RECENT;
    }

    private void attachFragment() {
        RecentStationsFragment fragment = RecentStationsFragment.create();
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

}
