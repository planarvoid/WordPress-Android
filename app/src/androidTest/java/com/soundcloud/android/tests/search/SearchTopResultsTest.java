package com.soundcloud.android.tests.search;

import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.discovery.OldDiscoveryScreen;
import com.soundcloud.android.screens.discovery.SearchTopResultsScreen;
import com.soundcloud.android.screens.discovery.SearchTrackResultsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class SearchTopResultsTest extends ActivityTest<MainActivity> {

    private OldDiscoveryScreen discoveryScreen;

    public SearchTopResultsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.searchUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        discoveryScreen = mainNavHelper.goToOldDiscovery();
    }

    @Override
    protected void beforeStartActivity() {
        getFeatureFlags().enable(Flag.SEARCH_TOP_RESULTS);
        getFeatureFlags().disable(Flag.DISCOVER_BACKEND);
    }

    @Override
    public void tearDown() throws Exception {
        getFeatureFlags().reset(Flag.SEARCH_TOP_RESULTS);
        getFeatureFlags().reset(Flag.DISCOVER_BACKEND);
        super.tearDown();
    }

    // https://soundcloud.atlassian.net/browse/DROID-1361
    @Ignore // Test assumes that Go+ tracks bucket is above Tracks bucket, but backend A/B Tests can change this
    public void testTopResultsUpgradeAndTracks() throws Exception {
        mrLocalLocal.startEventTracking();
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
        VisualPlayerElement playerElement = topResultsScreen.findAndClickFirstTrackItem();
        assertThat("Player should be expanded", playerElement.isExpanded());

        mrLocalLocal.verify("specs/search_top_results_upgrade_and_tracks.spec");
    }

    public void testTopResultsProfileAndAlbum() throws Exception {
        mrLocalLocal.startEventTracking();
        SearchTopResultsScreen topResultsScreen = discoveryScreen.clickSearch().doSearchTopResults("coldplay");
        assertThat("Search top results buckets screen should be visible", topResultsScreen.isVisible());

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
        mrLocalLocal.verify("specs/search_top_results_profile_and_album.spec");
    }
}
