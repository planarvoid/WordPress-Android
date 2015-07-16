package com.soundcloud.android.tests.search;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.screens.search.SearchResultsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class SearchNavigationTest extends ActivityTest<MainActivity> {
    private StreamScreen streamScreen;
    private PlaylistTagsScreen playlistTagsScreen;

    public SearchNavigationTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.FOLLOW_USER_SEARCH);
        super.setUp();

        streamScreen = new StreamScreen(solo);
        playlistTagsScreen = streamScreen.actionBar().clickSearchButton();
    }

    public void testShouldOpenSearchPageWhenClickingOnSearchIcon() {
        assertThat(playlistTagsScreen, is(visible()));
    }

    public void testGoingBackFromSearchResultsReturnsToTagPage() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        resultsScreen.pressBack();
        playlistTagsScreen = new PlaylistTagsScreen(solo);
        assertEquals("Tags screen should be visible", true, playlistTagsScreen.isVisible());
        assertEquals("Search query should be empty", "", playlistTagsScreen.actionBar().getSearchQuery());
    }

    public void testShouldExitSeachWhenPressingBackButton() {
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("clownstep");
        resultsScreen.pressBack();
        playlistTagsScreen = new PlaylistTagsScreen(solo);
        streamScreen = playlistTagsScreen.pressBack();
        assertEquals("Main screen should be visible", true, streamScreen.isVisible());
    }
}
