package com.soundcloud.android.tests.discovery;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.android.main.MainActivity;

import android.content.Context;

public class TrackRecommendationsTrackingTest extends TrackingActivityTest<MainActivity> {
    private static final String START_TRACK_RECOMMENDATIONS_FROM_REASON = "start_track_recommendations_from_reason";
    private static final String START_TRACK_RECOMMENDATIONS_FROM_VIEW_ALL = "start_track_recommendations_view_all";

    public TrackRecommendationsTrackingTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Context context = getInstrumentation().getTargetContext();
        ConfigurationHelper.disableIntroductoryOverlay(context, IntroductoryOverlayKey.PLAY_QUEUE);
    }

    @Override
    protected void logInHelper() {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());
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
