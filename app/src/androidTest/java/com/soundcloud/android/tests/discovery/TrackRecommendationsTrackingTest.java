package com.soundcloud.android.tests.discovery;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.ActivityTest;

public class TrackRecommendationsTrackingTest extends ActivityTest<MainActivity> {
    private static final String START_TRACK_RECOMMENDATIONS_FROM_REASON = "specs/start_track_recommendations_from_reason.spec";
    private static final String START_TRACK_RECOMMENDATIONS_FROM_VIEW_ALL = "specs/start_track_recommendations_view_all.spec";

    public TrackRecommendationsTrackingTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.defaultUser;
    }

    public void testStartPlaybackFromReasonOnDiscoveryScreen() throws Exception {
        mrLocalLocal.startEventTracking();

        mainNavHelper.goToOldDiscovery()
                     .trackRecommendationsBucket()
                     .clickReason()
                     .waitForExpandedPlayerToStartPlaying()
                     .clickArtwork();

        mrLocalLocal.verify(START_TRACK_RECOMMENDATIONS_FROM_REASON);
    }

    public void testStartPlaybackFromRecommendationOnViewAllScreen() throws Exception {
        mrLocalLocal.startEventTracking();

        mainNavHelper.goToOldDiscovery()
                     .trackRecommendationsBucket()
                     .clickViewAll()
                     .trackRecommendationsBucket()
                     .clickFirstRecommendedTrack()
                     .waitForExpandedPlayerToStartPlaying()
                     .clickArtwork();

        mrLocalLocal.verify(START_TRACK_RECOMMENDATIONS_FROM_VIEW_ALL);
    }
}
