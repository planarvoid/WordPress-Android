package com.soundcloud.android.tests.discovery;

import static com.soundcloud.android.framework.TestUser.defaultUser;
import static com.soundcloud.android.properties.Flag.DISCOVER_BACKEND;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class TrackRecommendationsTrackingTest extends ActivityTest<MainActivity> {
    private static final String START_TRACK_RECOMMENDATIONS_FROM_REASON = "specs/start_track_recommendations_from_reason.spec";
    private static final String START_TRACK_RECOMMENDATIONS_FROM_VIEW_ALL = "specs/start_track_recommendations_view_all.spec";

    public TrackRecommendationsTrackingTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return defaultUser;
    }

    @Override
    protected void beforeActivityLaunched() {
        getFeatureFlags().disable(DISCOVER_BACKEND);
    }

    @Override
    public void tearDown() throws Exception {
        getFeatureFlags().reset(DISCOVER_BACKEND);
        super.tearDown();
    }

    @Test
    public void testStartPlaybackFromReasonOnDiscoveryScreen() throws Exception {
        mrLocalLocal.startEventTracking();

        mainNavHelper.goToOldDiscovery()
                     .trackRecommendationsBucket()
                     .clickReason()
                     .waitForExpandedPlayerToStartPlaying()
                     .clickArtwork();

        mrLocalLocal.verify(START_TRACK_RECOMMENDATIONS_FROM_REASON);
    }

    @Test
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
