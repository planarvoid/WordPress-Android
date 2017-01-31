package com.soundcloud.android.discovery.charts;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class ChartActivity extends PlayerActivity {

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject @LightCycle ChartPresenter presenter;

    public static Intent createIntent(Context context, Urn genre, ChartType type, ChartCategory category, String header) {
        return new Intent(context, ChartActivity.class)
                .setAction(Intent.ACTION_VIEW)
                .putExtra(ChartTracksFragment.EXTRA_GENRE_URN, genre)
                .putExtra(ChartTracksFragment.EXTRA_TYPE, type)
                .putExtra(ChartTracksFragment.EXTRA_CATEGORY, category)
                .putExtra(ChartTracksFragment.EXTRA_HEADER, header);

    }

    public ChartActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
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
