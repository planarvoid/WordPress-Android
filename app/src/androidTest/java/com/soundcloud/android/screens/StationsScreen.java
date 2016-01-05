package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.StationsBucketElement;

public class StationsScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;

    public StationsScreen(Han solo) {
        super(solo);
        waiter.waitForFragmentByTag("stations_fragment");
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public StationsBucketElement getSavedStationsBucket() {
        return getBucket(With.text(testDriver.getString(R.string.stations_collection_title_saved_stations)));
    }

    public StationsBucketElement getRecentStationsBucket() {
        return getBucket(With.text(testDriver.getString(R.string.stations_collection_title_recent_stations)));
    }

    public StationsBucketElement getTrackRecommendationsBucket() {
        return getBucket(With.text(testDriver.getString(R.string.stations_collection_title_track_recommendations)));
    }

    public StationsBucketElement getCuratorRecommendationsBucket() {
        return getBucket(With.text(testDriver.getString(R.string.stations_collection_title_curator_recommendations)));
    }

    public StationsBucketElement getGenreRecommendationsBucket() {
        return getBucket(With.text(testDriver.getString(R.string.stations_collection_title_genre_recommendations)));
    }

    public StationsBucketElement getBucket(With child) {
        final RecyclerViewElement buckets = stationsBucketsListElement();
        return new StationsBucketElement(testDriver, buckets.scrollToItem(new MyCriteria(child)));
    }

    private RecyclerViewElement stationsBucketsListElement() {
        return testDriver
                .findElement(With.id(R.id.ak_recycler_view))
                .toRecyclerView();
    }

    private static class MyCriteria implements RecyclerViewElement.Criteria {
        private final With matcher;

        public MyCriteria(With matcher) {
            this.matcher = matcher;
        }

        @Override
        public boolean isSatisfied(ViewElement viewElement) {
            return !viewElement.findElements(matcher).isEmpty();
        }

        @Override
        public String description() {
            return matcher.getSelector();
        }
    }
}
