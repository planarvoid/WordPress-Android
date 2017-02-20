package com.soundcloud.android.tests.discovery;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.discovery.ViewAllTrackRecommendationsScreen;
import com.soundcloud.android.screens.elements.TrackRecommendationsBucketElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class TrackRecommendationsTest extends ActivityTest<MainActivity> {
    private DiscoveryScreen discoveryScreen;

    public TrackRecommendationsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.defaultUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        discoveryScreen = mainNavHelper.goToDiscovery();
    }

    public void testClickOnViewAllTrackRecommendations() {
        final ViewAllTrackRecommendationsScreen viewAllScreen = discoveryScreen.trackRecommendationsBucket()
                                                                               .clickViewAll();

        assertThat(viewAllScreen, is(visible()));
    }

    public void testStartPlaybackFromReasonOnDiscoveryScreen() {
        final TrackRecommendationsBucketElement bucket = discoveryScreen.trackRecommendationsBucket();
        final String reason = bucket.getReason();
        final String firstRecommendedTrackTitle = bucket.getFirstRecommendedTrackTitle();
        final VisualPlayerElement player = bucket.clickReason();

        player.waitForExpandedPlayer();

        assertThat(reason, containsString(player.getTrackTitle()));

        player.swipeNext();

        assertThat(firstRecommendedTrackTitle, equalTo(player.getTrackTitle()));
    }

    public void testStartPlaybackFromRecommendationOnDiscoveryScreen() {
        final TrackRecommendationsBucketElement bucket = discoveryScreen.trackRecommendationsBucket();
        final String firstRecommendedTrackTitle = bucket.getFirstRecommendedTrackTitle();
        final VisualPlayerElement player = bucket.clickFirstRecommendedTrack();

        player.waitForExpandedPlayerToStartPlaying();

        assertThat(firstRecommendedTrackTitle, equalTo(player.getTrackTitle()));
    }

    public void testStartPlaybackFromReasonOnViewAllScreen() {
        final ViewAllTrackRecommendationsScreen viewAllScreen = discoveryScreen.trackRecommendationsBucket()
                                                                               .clickViewAll();

        final TrackRecommendationsBucketElement bucket = viewAllScreen.trackRecommendationsBucket();
        final String reason = bucket.getReason();
        final String firstRecommendedTrackTitle = bucket.getFirstRecommendedTrackTitle();
        final VisualPlayerElement player = bucket.clickReason();

        assertThat(reason, containsString(player.getTrackTitle()));

        player.waitForExpandedPlayer();
        player.swipeNext();

        assertThat(firstRecommendedTrackTitle, equalTo(player.getTrackTitle()));
    }

    public void testStartPlaybackFromRecommendationOnViewAllScreen() {
        final ViewAllTrackRecommendationsScreen viewAllScreen = discoveryScreen.trackRecommendationsBucket()
                                                                               .clickViewAll();

        final TrackRecommendationsBucketElement bucket = viewAllScreen.trackRecommendationsBucket();
        final String firstRecommendedTrackTitle = bucket.getFirstRecommendedTrackTitle();
        final VisualPlayerElement player = bucket.clickFirstRecommendedTrack();

        assertThat(firstRecommendedTrackTitle, equalTo(player.getTrackTitle()));
    }
}
