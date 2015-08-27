package com.soundcloud.android.stations;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;

import javax.inject.Inject;

public class ShowAllStationsActivity extends ScActivity {
    public static final String TYPE = "type";
    public static final int RECENT = 0;

    @Inject @LightCycle PlayerController playerController;

    @Override
    protected void setContentView() {
        presenter.setBaseLayout();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle(this.getString(R.string.recent_stations_title));

        if (savedInstanceState == null) {
            attachFragment();
        }
    }

    private void attachFragment() {
        ShowAllStationsFragment fragment = ShowAllStationsFragment.create(getIntent().getIntExtra(TYPE, Consts.NOT_SET));
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }
}
