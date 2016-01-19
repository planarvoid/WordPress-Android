package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.StationsBucketElement;

public class StationsScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;

    public StationsScreen(Han testDriver) {
        super(testDriver);
        waiter.waitForFragmentByTag("stations_fragment");
        pullToRefresh();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public StationsBucketElement getSavedStationsBucket() {
        return scrollToBucket(testDriver.getString(R.string.stations_collection_title_saved_stations));
    }

    public StationsBucketElement getRecentStationsBucket() {
        return scrollToBucket(testDriver.getString(R.string.stations_collection_title_recent_stations));
    }

    public StationsBucketElement getTrackRecommendationsBucket() {
        return scrollToBucket(testDriver.getString(R.string.stations_collection_title_track_recommendations));
    }

    public StationsBucketElement getCuratorRecommendationsBucket() {
        return scrollToBucket(testDriver.getString(R.string.stations_collection_title_curator_recommendations));
    }

    public StationsBucketElement getGenreRecommendationsBucket() {
        return scrollToBucket(testDriver.getString(R.string.stations_collection_title_genre_recommendations));
    }

    private StationsBucketElement scrollToBucket(String title) {
        ViewElement result = testDriver.scrollToItem(
                With.id(R.id.stations_bucket),
                StationsBucketElement.WithTitle(testDriver, title)
        );

        return new StationsBucketElement(testDriver, result);
    }
}
