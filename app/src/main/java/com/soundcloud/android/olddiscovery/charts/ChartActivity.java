package com.soundcloud.android.olddiscovery.charts;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.deeplinks.ChartDetails;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class ChartActivity extends PlayerActivity {

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject @LightCycle ChartPresenter presenter;

    public static Intent createIntent(Context context, ChartDetails chartDetails) {
        final Intent intent = new Intent(context, ChartActivity.class)
                .setAction(Intent.ACTION_VIEW)
                .putExtra(ChartTracksFragment.EXTRA_TYPE, chartDetails.type())
                .putExtra(ChartTracksFragment.EXTRA_CATEGORY, chartDetails.category())
                .putExtra(ChartTracksFragment.EXTRA_HEADER, chartDetails.title().or(""));
        return Urns.writeToIntent(intent, ChartTracksFragment.EXTRA_GENRE_URN, chartDetails.genre());

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
