package com.soundcloud.android.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

public class StationInfoActivity extends PlayerActivity {

    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_URN = "urn";
    public static final String EXTRA_SEED_URN = "seed_urn";

    @Inject BaseLayoutHelper baseLayoutHelper;

    public StationInfoActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            final Urn stationUrn = getIntent().getParcelableExtra(EXTRA_URN);
            final Urn seedTrackUrn = getIntent().getParcelableExtra(EXTRA_SEED_URN);
            final String source = getIntent().getStringExtra(EXTRA_SOURCE);

            final Fragment fragment = StationInfoFragment.create(stationUrn, seedTrackUrn, source);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
        }
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayout(this);
    }

    @Override
    public Screen getScreen() {
        return Screen.STATIONS_INFO;
    }
}
