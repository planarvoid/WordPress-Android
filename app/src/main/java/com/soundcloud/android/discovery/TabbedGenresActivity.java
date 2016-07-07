package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;

import javax.inject.Inject;

public class TabbedGenresActivity extends PlayerActivity {
    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject @LightCycle TabbedGenresPresenter presenter;

    @Override
    public Screen getScreen() {
        return Screen.ALL_GENRES;
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.createActionBarLayout(this, R.layout.tabbed_activity);
    }
}
