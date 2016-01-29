package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetOfflineSyncState;
import static com.soundcloud.android.screens.elements.DownloadImageViewElement.IsDownloading.downloading;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.OfflinePlaylistsTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.elements.DownloadImageViewElement;
import com.soundcloud.android.screens.elements.PlaylistElement;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

@OfflinePlaylistsTest
public class OfflinePlaylistTest extends ActivityTest<MainActivity> {

    public OfflinePlaylistTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.offlineUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        final Context context = getInstrumentation().getTargetContext();
        resetOfflineSyncState(context);
        enableOfflineContent(context);
    }

    public void testDownloadsPlaylistWhenMadeAvailableOfflineFromItem() {
        final PlaylistElement firstPlaylist = mainNavHelper
                .goToCollections()
                .scrollToPlaylistWithTitle("Offline playlist");
        firstPlaylist.clickOverflow().clickMakeAvailableOffline();

        final DownloadImageViewElement downloadElement = firstPlaylist.downloadElement();

        assertThat(downloadElement, is(downloading()));
    }

    public void testDownloadPlaylistWhenMadeAvailableOfflineFromPlaylistDetails() {
        final PlaylistDetailsScreen playlistDetailsScreen = mainNavHelper.goToCollections()
                .scrollToAndClickPlaylistWithTitle("Offline playlist")
                .clickPlaylistOverflowButton()
                .clickMakeAvailableOffline();

        final DownloadImageViewElement downloadElement = playlistDetailsScreen.headerDownloadElement();
        assertThat(downloadElement, is(downloading()));
    }

    public void testEmptyPlaylistsAreMarkedAsRequested() throws Exception {
        final PlaylistDetailsScreen playlistDetailsScreen = mainNavHelper.goToCollections()
                .scrollToAndClickPlaylistWithTitle("Empty playlist")
                .clickPlaylistOverflowButton()
                .clickMakeAvailableOffline();

        final DownloadImageViewElement downloadElement = playlistDetailsScreen.headerDownloadElement();
        assertThat(downloadElement, is(not(downloading())));
        assertThat("Playlist should be requested ", downloadElement.isRequested());
    }

    public void testUnavailablePlaylistsAreMarkedAsUnavailable() throws Exception {
        final PlaylistDetailsScreen playlistDetailsScreen = mainNavHelper.goToCollections()
                .scrollToAndClickPlaylistWithTitle("Unavailable playlist")
                .clickPlaylistOverflowButton()
                .clickMakeAvailableOffline();

        final DownloadImageViewElement downloadElement = playlistDetailsScreen.headerDownloadElement();
        assertThat("Playlist should be unavailable ", downloadElement.isUnavailable());
    }

    public void testPlaylistsWithAvailableTracksAreNotMarkedAsUnavailable() throws Exception {
        final PlaylistDetailsScreen playlistDetailsScreen = mainNavHelper.goToCollections()
                .scrollToAndClickPlaylistWithTitle("Mixed playlist")
                .clickPlaylistOverflowButton()
                .clickMakeAvailableOffline();

        final DownloadImageViewElement downloadElement = playlistDetailsScreen.headerDownloadElement();
        assertThat("Playlist should not be unavailable ", !downloadElement.isUnavailable());
    }

    public void testPlaylistIsRequestedWhenNetworkIsOff() throws Exception {
        final PlaylistElement playlist = mainNavHelper.goToCollections().scrollToPlaylistWithTitle("Offline playlist");
        networkManagerClient.switchWifiOff();
        playlist
                .clickOverflow()
                .clickMakeAvailableOffline();

        final DownloadImageViewElement downloadElement = playlist.downloadElement();
        assertThat(downloadElement, is(not(downloading())));
        assertThat("Playlist should be requested ", downloadElement.isRequested());
    }
}
