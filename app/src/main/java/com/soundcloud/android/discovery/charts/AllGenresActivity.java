package com.soundcloud.android.discovery.charts;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public class AllGenresActivity extends PlayerActivity {
    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject @LightCycle AllGenresPresenter presenter;

    public AllGenresActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public Screen getScreen() {
        return presenter.getScreen();
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.createActionBarLayout(this, R.layout.tabbed_activity);
    }
}
