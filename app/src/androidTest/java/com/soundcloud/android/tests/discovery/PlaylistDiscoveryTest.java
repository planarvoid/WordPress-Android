package com.soundcloud.android.tests.discovery;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.Consts;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.discovery.PlaylistResultsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class PlaylistDiscoveryTest extends ActivityTest<MainActivity> {

    private DiscoveryScreen discoveryScreen;

    public PlaylistDiscoveryTest() {
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

    public void testPlaylistDiscoveryTags() {
        String tagTitle = discoveryScreen.getTagTitle(5);
        PlaylistResultsScreen resultsScreen = discoveryScreen.clickOnTag(5);

        assertPlaylistResultsScreenShown(tagTitle, resultsScreen);
        assertPlaylistDetailsScreen(resultsScreen);
        assertBackNavigationToDiscovery(resultsScreen);
    }

    private void assertBackNavigationToDiscovery(PlaylistResultsScreen resultsScreen) {
        resultsScreen.pressBack();
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
        assertThat("Playlist results should not be empty", resultsScreen.getResultsCount(), is(
                Consts.CARD_PAGE_SIZE));
    }
}
