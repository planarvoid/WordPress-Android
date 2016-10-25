package com.soundcloud.android.discovery.charts;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;

import javax.inject.Inject;

public class ChartActivity extends PlayerActivity {
    public static final String EXTRA_HEADER = "chartHeader";

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject @LightCycle ChartPresenter presenter;

    public ChartActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getIntent().getStringExtra(EXTRA_HEADER));
    }

    @Override
    public Screen getScreen() {
        return Screen.UNKNOWN;
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.createActionBarLayout(this, R.layout.tabbed_activity);
    }
}
