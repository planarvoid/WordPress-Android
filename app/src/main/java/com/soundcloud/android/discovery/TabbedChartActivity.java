package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;

import javax.inject.Inject;

public class TabbedChartActivity extends PlayerActivity {
    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject @LightCycle TabbedChartPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.charts_header);
    }

    @Override
    public Screen getScreen() {
        return Screen.TOP_CHARTS;
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.createActionBarLayout(this, R.layout.tabbed_top_charts_activity);
    }
}
