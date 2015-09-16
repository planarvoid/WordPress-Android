package com.soundcloud.android.stations;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;

import javax.inject.Inject;

public class ShowAllStationsActivity extends ScActivity {
    public static final String COLLECTION_TYPE = "type";

    @Inject @LightCycle PlayerController playerController;

    @Override
    protected void setContentView() {
        presenter.setBaseLayout();
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
                return getString(R.string.stations_collection_title_recently_played_stations);
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

    private void attachFragment() {
        ShowAllStationsFragment fragment = ShowAllStationsFragment.create(getIntent().getIntExtra(COLLECTION_TYPE, Consts.NOT_SET));
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }
}
