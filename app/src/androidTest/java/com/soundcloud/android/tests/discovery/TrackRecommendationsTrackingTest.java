package com.soundcloud.android.tests.discovery;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;

public class TrackRecommendationsTrackingTest extends TrackingActivityTest<MainActivity> {
    private static final String START_TRACK_RECOMMENDATIONS_FROM_REASON = "start_track_recommendations_from_reason";
    private static final String START_TRACK_RECOMMENDATIONS_FROM_VIEW_ALL = "start_track_recommendations_view_all";

    public TrackRecommendationsTrackingTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.defaultUser;
    }

    public void testStartPlaybackFromReasonOnDiscoveryScreen() {
        startEventTracking();

        mainNavHelper.goToDiscovery()
                     .trackRecommendationsBucket()
                     .clickReason()
                     .waitForExpandedPlayerToStartPlaying()
                     .clickArtwork();

        finishEventTracking(START_TRACK_RECOMMENDATIONS_FROM_REASON);
    }

    public void testStartPlaybackFromRecommendationOnViewAllScreen() {
        startEventTracking();

        mainNavHelper.goToDiscovery()
                     .trackRecommendationsBucket()
                     .clickViewAll()
                     .trackRecommendationsBucket()
                     .clickFirstRecommendedTrack()
                     .waitForExpandedPlayerToStartPlaying()
                     .clickArtwork();

        finishEventTracking(START_TRACK_RECOMMENDATIONS_FROM_VIEW_ALL);
    }
}
