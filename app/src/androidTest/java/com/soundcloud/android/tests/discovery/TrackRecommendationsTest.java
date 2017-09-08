package com.soundcloud.android.tests.discovery;

import static com.soundcloud.android.framework.TestUser.defaultUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.properties.Flag.DISCOVER_BACKEND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.discovery.OldDiscoveryScreen;
import com.soundcloud.android.screens.discovery.ViewAllTrackRecommendationsScreen;
import com.soundcloud.android.screens.elements.TrackRecommendationsBucketElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class TrackRecommendationsTest extends ActivityTest<MainActivity> {
    private OldDiscoveryScreen discoveryScreen;

    public TrackRecommendationsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return defaultUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        discoveryScreen = mainNavHelper.goToOldDiscovery();
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
    public void testClickOnViewAllTrackRecommendations() throws Exception {
        final ViewAllTrackRecommendationsScreen viewAllScreen = discoveryScreen.trackRecommendationsBucket()
                                                                               .clickViewAll();

        assertThat(viewAllScreen, is(visible()));
    }

    @Test
    public void testStartPlaybackFromReasonOnDiscoveryScreen() throws Exception {
        final TrackRecommendationsBucketElement bucket = discoveryScreen.trackRecommendationsBucket();
        final String reason = bucket.getReason();
        final String firstRecommendedTrackTitle = bucket.getFirstRecommendedTrackTitle();
        final VisualPlayerElement player = bucket.clickReason();

        player.waitForExpandedPlayer();
        assertThat(reason, containsString(player.getTrackTitle()));

        player.swipeNext();
        assertThat(firstRecommendedTrackTitle, equalTo(player.getTrackTitle()));
    }

    @Test
    public void testStartPlaybackFromRecommendationOnDiscoveryScreen() throws Exception {
        final TrackRecommendationsBucketElement bucket = discoveryScreen.trackRecommendationsBucket();
        final String firstRecommendedTrackTitle = bucket.getFirstRecommendedTrackTitle();
        final VisualPlayerElement player = bucket.clickFirstRecommendedTrack();

        player.waitForExpandedPlayerToStartPlaying();
        assertThat(firstRecommendedTrackTitle, equalTo(player.getTrackTitle()));
    }

    @Test
    public void testStartPlaybackFromReasonOnViewAllScreen() throws Exception {
        final ViewAllTrackRecommendationsScreen viewAllScreen = discoveryScreen.trackRecommendationsBucket()
                                                                               .clickViewAll();

        final TrackRecommendationsBucketElement bucket = viewAllScreen.trackRecommendationsBucket();
        final String reason = bucket.getReason();
        final String firstRecommendedTrackTitle = bucket.getFirstRecommendedTrackTitle();
        final VisualPlayerElement player = bucket.clickReason();

        player.waitForExpandedPlayer();
        assertThat(reason, containsString(player.getTrackTitle()));

        player.swipeNext();
        assertThat(firstRecommendedTrackTitle, equalTo(player.getTrackTitle()));
    }

    @Test
    public void testStartPlaybackFromRecommendationOnViewAllScreen() throws Exception {
        final ViewAllTrackRecommendationsScreen viewAllScreen = discoveryScreen.trackRecommendationsBucket()
                                                                               .clickViewAll();

        final TrackRecommendationsBucketElement bucket = viewAllScreen.trackRecommendationsBucket();
        final String firstRecommendedTrackTitle = bucket.getFirstRecommendedTrackTitle();
        final VisualPlayerElement player = bucket.clickFirstRecommendedTrack();

        player.waitForExpandedPlayer();
        assertThat(firstRecommendedTrackTitle, equalTo(player.getTrackTitle()));
    }
}
