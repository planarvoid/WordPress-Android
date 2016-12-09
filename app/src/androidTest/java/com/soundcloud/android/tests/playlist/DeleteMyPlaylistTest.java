package com.soundcloud.android.tests.playlist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CreatePlaylistScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class DeleteMyPlaylistTest extends ActivityTest<MainActivity> {

    public DeleteMyPlaylistTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.deletePlaylistUser;
    }

    public void testDeletePlaylistFromOverFlowMenu() {
        final String newPlaylist = createNewPlaylist();
        final PlaylistsScreen playlistsScreen = mainNavHelper.goToCollections().clickPlaylistsPreview();

        playlistsScreen
                .scrollToPlaylistWithTitle(newPlaylist)
                .clickOverflow()
                .clickDelete()
                .clickConfirm();

        assertThat(playlistsScreen.getPlaylistWithTitle(newPlaylist).isOnScreen(), is(false));
    }

    public void testDeletePlaylistFromPlaylistDetails() {
        final String newPlaylist = createNewPlaylist();

        final PlaylistsScreen playlistsScreen = mainNavHelper.goToCollections().clickPlaylistsPreview();

        playlistsScreen
                .scrollToPlaylistWithTitle(newPlaylist)
                .click()
                .clickPlaylistOverflowButton()
                .clickDelete()
                .clickConfirm();

        assertThat(playlistsScreen.getPlaylistWithTitle(newPlaylist).isOnScreen(), is(false));
    }

    private String createNewPlaylist() {
        final String title = String.valueOf(System.currentTimeMillis());
        final CreatePlaylistScreen createPlaylistScreen = mainNavHelper.goToStream()
                                                                       .clickFirstNotPromotedTrackCard()
                                                                       .clickMenu()
                                                                       .clickAddToPlaylist()
                                                                       .clickCreateNewPlaylist()
                                                                       .enterTitle(title);

        // Fix : test can find a track in the stream because the
        // user fill up with playlists.
        //
        // This can happen because while the creation is synced, the deletion
        // may not be synced (it the test runner kill the app before)
        networkManagerClient.switchWifiOff();

        createPlaylistScreen
                .clickDoneAndReturnToPlayer()
                .pressCloseButton();

        return title;
    }

}
