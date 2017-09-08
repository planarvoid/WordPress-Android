package com.soundcloud.android.tests.discovery;

import static com.soundcloud.android.framework.TestUser.defaultUser;
import static com.soundcloud.android.properties.Flag.DISCOVER_BACKEND;
import static com.soundcloud.android.properties.Flag.RECOMMENDED_PLAYLISTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.discovery.OldDiscoveryScreen;
import com.soundcloud.android.screens.discovery.PlaylistResultsScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class PlaylistDiscoveryTest extends ActivityTest<MainActivity> {

    private OldDiscoveryScreen discoveryScreen;

    public PlaylistDiscoveryTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return defaultUser;
    }

    @Override
    protected void beforeActivityLaunched() {
        getFeatureFlags().disable(RECOMMENDED_PLAYLISTS);
        getFeatureFlags().disable(DISCOVER_BACKEND);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        discoveryScreen = mainNavHelper.goToOldDiscovery();
    }

    @Override
    public void tearDown() throws Exception {
        getFeatureFlags().reset(RECOMMENDED_PLAYLISTS);
        getFeatureFlags().reset(DISCOVER_BACKEND);
        super.tearDown();
    }

    @Test
    public void testPlaylistDiscoveryTags() throws Exception {
        String tagTitle = discoveryScreen.getTagTitle(5);
        PlaylistResultsScreen resultsScreen = discoveryScreen.clickOnTag(5);

        assertPlaylistResultsScreenShown(tagTitle, resultsScreen);
        assertPlaylistDetailsScreen(resultsScreen);
        assertBackNavigationToDiscovery(resultsScreen);
    }

    private void assertBackNavigationToDiscovery(PlaylistResultsScreen resultsScreen) {
        resultsScreen.goBack(OldDiscoveryScreen::new);
        assertThat("Main screen should be visible", discoveryScreen.isVisible());
        assertThat("Playlist tags should be visible", discoveryScreen.isDisplayingTags());
    }

    private void assertPlaylistDetailsScreen(PlaylistResultsScreen resultsScreen) {
        PlaylistDetailsScreen detailsScreen = resultsScreen.clickOnFirstPlaylist();
        assertThat("Playlist details screen should be shown", detailsScreen.isVisible());
        solo.goBack();
    }

    private void assertPlaylistResultsScreenShown(String tagTitle, PlaylistResultsScreen resultsScreen) {
        assertThat("Playlist results screen should be visible", resultsScreen.isVisible());
        assertThat("Screen should show clicked playlist tag as title", resultsScreen.getActionBarTitle(), is(tagTitle));
        assertThat("Playlist results should not be empty", resultsScreen.getResultsCount(), greaterThan(0));
    }
}
