package com.soundcloud.android.stations;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;

import javax.inject.Inject;

public class ShowAllStationsActivity extends ScActivity {
    public static final String COLLECTION_TYPE = "type";

    @Inject @LightCycle PlayerController playerController;

    @Inject BaseLayoutHelper baseLayoutHelper;

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayout(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle(getTitleFromIntent());

        if (savedInstanceState == null) {
            attachFragment();
        }
    }

    private String getTitleFromIntent() {
        final int type = getIntent().getIntExtra(COLLECTION_TYPE, Consts.NOT_SET);

        switch (type) {
            case StationsCollectionsTypes.SAVED:
                return getString(R.string.stations_collection_title_saved_stations);
            case StationsCollectionsTypes.RECENT:
                return getString(R.string.stations_collection_title_recent_stations);
            case StationsCollectionsTypes.CURATOR_RECOMMENDATIONS:
                return getString(R.string.stations_collection_title_curator_recommendations);
            case StationsCollectionsTypes.GENRE_RECOMMENDATIONS:
                return getString(R.string.stations_collection_title_genre_recommendations);
            case StationsCollectionsTypes.TRACK_RECOMMENDATIONS:
                return getString(R.string.stations_collection_title_track_recommendations);
            default:
                throw new IllegalStateException("Unknown StationsCollectionsType: " + type);
        }
    }

    @Override
    public Screen getScreen() {
        final int type = getIntent().getIntExtra(COLLECTION_TYPE, Consts.NOT_SET);
        if (type == StationsCollectionsTypes.RECENT) {
            return Screen.STATIONS_RECENT;
        }
        return Screen.STATIONS_SHOW_ALL;
    }

    private void attachFragment() {
        ShowAllStationsFragment fragment = ShowAllStationsFragment.create(getIntent().getIntExtra(COLLECTION_TYPE, Consts.NOT_SET));
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    @Override
    public void onBackPressed() {
        if (!playerController.handleBackPressed()) {
            super.onBackPressed();
        }
    }

}
