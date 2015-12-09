package com.soundcloud.android.tests.playlist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionsScreen;
import com.soundcloud.android.tests.ActivityTest;

@Ignore
public class DeleteMyPlaylistTest extends ActivityTest<MainActivity> {

    public DeleteMyPlaylistTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.deletePlaylistUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testDeletePlaylistFromOverFlowMenu() {
        final String newPlaylist = createANewPlaylist();
        final CollectionsScreen collectionsScreen = mainNavHelper.goToCollections();

        collectionsScreen
                .getPlaylist(newPlaylist)
                .clickOverflow()
                .clickDelete()
                .clickConfirm();

        assertThat(collectionsScreen.getPlaylist(newPlaylist).isVisible(), is(false));
    }

    public void testDeletePlaylistFromPlaylistDetails() {
        final String newPlaylist = createANewPlaylist();

        final CollectionsScreen collectionsScreen = mainNavHelper.goToCollections();

        collectionsScreen
                .getPlaylist(newPlaylist)
                .click()
                .clickPlaylistOverflowButton()
                .clickDelete()
                .clickConfirm();

        assertThat(collectionsScreen.getPlaylist(newPlaylist).isVisible(), is(false));
    }

    private String createANewPlaylist() {
        final String title = String.valueOf(System.currentTimeMillis());
        mainNavHelper.goToStream()
                .clickFirstNotPromotedTrackCard()
                .clickMenu()
                .clickAddToPlaylist()
                .clickCreateNewPlaylist()
                .enterTitle(title)
                .clickDoneAndReturnToPlayer()
                .pressCloseButton();

        return title;
    }

}
