package com.soundcloud.android.tests.discovery;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.Consts;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.framework.annotation.Issue;
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
    protected void logInHelper() {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        discoveryScreen = mainNavHelper.goToDiscovery();
    }

    @Ignore
    @Issue(ref = "https://github.com/soundcloud/SoundCloud-Android/issues/3800")
    public void testTagDisplayedAsSuggestionAfterTagSearch() {
        discoveryScreen.clickOnTag(1).pressBack();

        assertThat("Playlist tags screen should be visible", discoveryScreen.isVisible());
        assertThat("Searched tag should be in recents", discoveryScreen.playlistRecentTags().contains("#rap"));
    }

    public void testClickingOnPlaylistTagOpensPlaylistResultsScreenWithDefaultNumberOfResults() {
        PlaylistResultsScreen resultsScreen = discoveryScreen.clickOnTag(5);

        assertThat("Playlist results screen should be visible", resultsScreen.isVisible());
        assertThat("Playlist results should not be empty", resultsScreen.getResultsCount(), is(Consts.CARD_PAGE_SIZE));
    }

    public void testClickingOnPlaylistOpensPlaylistActivity() {
        PlaylistDetailsScreen detailsScreen = discoveryScreen.clickOnTag(5).clickOnPlaylist(0);

        assertThat("Playlist details screen should be shown", detailsScreen.isVisible());
    }

    public void testClickOnPlaylistTagOpensTagResults() {
        PlaylistResultsScreen resultsScreen = discoveryScreen.clickOnTag(1);

        assertThat("Screen should show clicked playlist tag as title", resultsScreen.getActionBarTitle(), is("#rap"));
    }

    public void testGoingBackFromPlayResultsReturnsToDiscoveryPage() {
        discoveryScreen.clickOnTag(5).pressBack();

        assertThat("Main screen should be visible", discoveryScreen.isVisible());
        assertThat("Playlist tags should be visible", discoveryScreen.isDisplayingTags());
    }
}
