package com.soundcloud.android.tests.playlist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class DeleteMyPlaylistTest extends ActivityTest<MainActivity> {

    public DeleteMyPlaylistTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.deletePlaylistUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testDeletePlaylistFromOverFlowMenu() {
        final String newPlaylist = createNewPlaylist();
        final CollectionsScreen collectionsScreen = mainNavHelper.goToCollections();

        collectionsScreen
                .getPlaylist(newPlaylist)
                .clickOverflow()
                .clickDelete()
                .clickConfirm();

        assertThat(collectionsScreen.getPlaylist(newPlaylist).isVisible(), is(false));
    }

    public void testDeletePlaylistFromPlaylistDetails() {
        final String newPlaylist = createNewPlaylist();

        final CollectionsScreen collectionsScreen = mainNavHelper.goToCollections();

        collectionsScreen
                .getPlaylist(newPlaylist)
                .click()
                .clickPlaylistOverflowButton()
                .clickDelete()
                .clickConfirm();

        assertThat(collectionsScreen.getPlaylist(newPlaylist).isVisible(), is(false));
    }

    private String createNewPlaylist() {
        final String title = String.valueOf(System.currentTimeMillis());
        mainNavHelper
                .goToDiscovery()
                .clickSearch()
                .doSearch("lots o' comments")
                .clickFirstTrackOverflowButton()
                .clickAddToPlaylist()
                .clickCreateNewPlaylist()
                .enterTitle(title)
                .clickDoneAndReturnToSearchResultsScreen()
                .goBack();

        return title;
    }

}
