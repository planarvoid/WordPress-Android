package com.soundcloud.android.tests.search;

import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.FeatureFlagsHelper;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.discovery.SearchTopResultsScreen;
import com.soundcloud.android.screens.discovery.SearchTrackResultsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class SearchTopResultsTest extends TrackingActivityTest<MainActivity> {

    private DiscoveryScreen discoveryScreen;
    private FeatureFlagsHelper featureFlagsHelper;

    public SearchTopResultsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.topResultsTestUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        featureFlagsHelper = FeatureFlagsHelper.create(getInstrumentation().getTargetContext());
        featureFlagsHelper.enable(Flag.SEARCH_TOP_RESULTS);
        discoveryScreen = mainNavHelper.goToDiscovery();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        featureFlagsHelper.reset(Flag.SEARCH_TOP_RESULTS);
    }

    public void testTopResults() throws Exception {
        SearchTopResultsScreen topResultsScreen = discoveryScreen.clickSearch().doSearchTopResults("coldplay");
        assertThat("Search top results buckets screen should be visible", topResultsScreen.isVisible());

        assertTrue(topResultsScreen.goTracksHeader().hasVisibility());
        SearchTrackResultsScreen trackResultsScreen = topResultsScreen.clickSeeAllGoTracksButton();
        assertThat("Search results screen should be visible", trackResultsScreen.isVisible());
        assertTrue(trackResultsScreen.goTracksCountHeader().hasVisibility());
        UpgradeScreen upgradeScreen = trackResultsScreen.clickOnUpgradeSubscription();
        assertThat("Upgrade subscription screen should be visible", upgradeScreen.isVisible());
        upgradeScreen.goBack();
        topResultsScreen = trackResultsScreen.pressBack();
        assertThat("Search top results buckets screen should be visible", topResultsScreen.isVisible());

        assertTrue(topResultsScreen.tracksHeader().hasVisibility());
        VisualPlayerElement playerElement = topResultsScreen.findAndClickFirstTrackItem().pressBackToCollapse();
        assertThat("Player is collapsed", playerElement.isCollapsed());
        assertThat("Search results screen should be visible", topResultsScreen.isVisible());

        assertTrue(topResultsScreen.peopleHeader().hasVisibility());
        ProfileScreen profileScreen = topResultsScreen.findAndClickFirstUserItem();
        assertThat("Profile screen should be visible", profileScreen.isVisible());
        profileScreen.goBack();
        assertThat("Search results screen should be visible", topResultsScreen.isVisible());

        assertTrue(topResultsScreen.albumHeader().hasVisibility());
        PlaylistDetailsScreen albumScreen = topResultsScreen.findAndClickFirstAlbumItem();
        assertThat("Album screen should be visible", albumScreen.isVisible());
        albumScreen.goBack();
        assertThat("Search results screen should be visible", topResultsScreen.isVisible());

        assertTrue(topResultsScreen.playlistHeader().hasVisibility());
    }
}
