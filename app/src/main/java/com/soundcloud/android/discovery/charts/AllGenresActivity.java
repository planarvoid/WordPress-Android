package com.soundcloud.android.discovery.charts;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class AllGenresActivity extends PlayerActivity {
    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject @LightCycle AllGenresPresenter presenter;

    public static Intent createIntent(Context context, ChartCategory category) {
        return new Intent(context, AllGenresActivity.class)
                .setAction(Intent.ACTION_VIEW)
                .putExtra(AllGenresPresenter.EXTRA_CATEGORY, category);
    }

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
