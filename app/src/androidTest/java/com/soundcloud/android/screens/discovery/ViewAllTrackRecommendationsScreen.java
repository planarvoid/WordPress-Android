package com.soundcloud.android.screens.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.TrackRecommendationsBucketElement;

public class ViewAllTrackRecommendationsScreen extends Screen {
    private static final Class ACTIVITY = ViewAllTrackRecommendationsScreen.class;

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public ViewAllTrackRecommendationsScreen(Han solo) {
        super(solo);
        waitForFragment();
    }

    @Override
    public boolean isVisible() {
        return waitForFragment();
    }

    private boolean waitForFragment() {
        return waiter.waitForFragmentByTag("ViewAllRecommendedTracksTag");
    }

    public TrackRecommendationsBucketElement trackRecommendationsBucket() {
        final ViewElement firstBucket = testDriver.findOnScreenElement(With.id(R.id.track_recommendations_bucket));

        return new TrackRecommendationsBucketElement(testDriver, firstBucket);
    }
}
