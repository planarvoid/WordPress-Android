package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetOfflineSyncState;
import static com.soundcloud.android.screens.elements.DownloadImageViewElement.IsDownloaded.downloaded;
import static com.soundcloud.android.screens.elements.DownloadImageViewElement.IsDownloading.downloading;
import static com.soundcloud.android.screens.elements.DownloadImageViewElement.IsDownloadingOrDownloaded.downloadingOrDownloaded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.elements.DownloadImageViewElement;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

public class OfflinePlaylistTest extends ActivityTest<MainActivity> {
    public static final String TAG = "OfflineTests";
    private static final String UNAVAILABLE_PLAYLIST = "Unavailable playlist";
    private static final String EMPTY_PLAYLIST = "Empty playlist";
    private static final String MIXED_PLAYLIST = "Mixed playlist";
    private static final String OFFLINE_PLAYLIST = "Offline playlist";

    public OfflinePlaylistTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.offlineUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        final Context context = getInstrumentation().getTargetContext();
        resetOfflineSyncState(context);
        enableOfflineContent(context);
    }

    public void testDownloadsPlaylistWhenMadeAvailableOfflineFromItem() {
        final PlaylistsScreen playlistsScreen = mainNavHelper
                .goToCollections()
                .clickPlaylistsPreview();

        playlistsScreen
                .scrollToPlaylistWithTitle("Offline playlist")
                .clickOverflow()
                .clickMakeAvailableOffline();

        PlaylistDetailsScreen playlistDetailsScreen =
                playlistsScreen.scrollToPlaylistWithTitle("Offline playlist").click();

        assertThat(playlistDetailsScreen.headerDownloadElement(), is(downloadingOrDownloaded()));
    }

    public void testDownloadsPlaylistWhenMadeAvailableOfflineFromDetails() {
        final PlaylistsScreen playlistsScreen = mainNavHelper
                .goToCollections()
                .clickPlaylistsPreview();

        final PlaylistDetailsScreen playlistDetailsScreen = playlistsScreen
                .scrollToAndClickPlaylistWithTitle(OFFLINE_PLAYLIST)
                .clickDownloadToggle();

        assertThat(playlistDetailsScreen.headerDownloadElement(), is(downloadingOrDownloaded()));

        final DownloadImageViewElement collectionsDownloadElement = playlistDetailsScreen
                .goBackToPlaylists()
                .scrollToPlaylistWithTitle(OFFLINE_PLAYLIST)
                .downloadElement();
        assertThat(collectionsDownloadElement, is(downloadingOrDownloaded()));
    }

    public void testDownloadPlaylistWhenMadeAvailableOfflineFromPlaylistDetails() {
        final PlaylistDetailsScreen playlistDetailsScreen = mainNavHelper.goToCollections()
                                                                         .clickPlaylistsPreview()
                                                                         .scrollToAndClickPlaylistWithTitle(
                                                                                 OFFLINE_PLAYLIST)
                                                                         .clickDownloadToggle();

        final DownloadImageViewElement downloadElement = playlistDetailsScreen.headerDownloadElement();
        assertThat(downloadElement, is(downloading()));
        playlistDetailsScreen.waitForDownloadToFinish();
        assertThat(downloadElement, is(downloaded()));

        final DownloadImageViewElement collectionsDownloadElement = playlistDetailsScreen
                .goBackToPlaylists()
                .scrollToPlaylistWithTitle(OFFLINE_PLAYLIST)
                .downloadElement();
        assertThat(collectionsDownloadElement, is(downloaded()));
    }

    public void testEmptyPlaylistsAreMarkedAsRequested() {
        final PlaylistDetailsScreen playlistDetailsScreen = mainNavHelper.goToCollections()
                                                                         .clickPlaylistsPreview()
                                                                         .scrollToAndClickPlaylistWithTitle(
                                                                                 EMPTY_PLAYLIST)
                                                                         .clickDownloadToggle();

        final DownloadImageViewElement downloadElement = playlistDetailsScreen.headerDownloadElement();
        assertThat(downloadElement, is(not(downloading())));
        assertThat("Playlist should be requested ", downloadElement.isRequested());

        final DownloadImageViewElement collectionsDownloadElement = playlistDetailsScreen
                .goBackToPlaylists()
                .scrollToPlaylistWithTitle(EMPTY_PLAYLIST)
                .downloadElement();
        assertThat(collectionsDownloadElement, is(not(downloading())));
        assertThat("Playlist should be requested ", collectionsDownloadElement.isRequested());
    }

    public void testUnavailablePlaylistsAreMarkedAsUnavailable() {
        final PlaylistDetailsScreen playlistDetailsScreen = mainNavHelper.goToCollections()
                                                                         .clickPlaylistsPreview()
                                                                         .scrollToAndClickPlaylistWithTitle(
                                                                                 UNAVAILABLE_PLAYLIST)
                                                                         .clickDownloadToggle();

        final DownloadImageViewElement downloadElement = playlistDetailsScreen.headerDownloadElement();
        assertThat(downloadElement, is(not(downloading())));
        assertThat("Playlist should be unavailable ", downloadElement.isUnavailable());

        final DownloadImageViewElement collectionsDownloadElement =
                playlistDetailsScreen
                        .goBackToPlaylists()
                        .scrollToPlaylistWithTitle(UNAVAILABLE_PLAYLIST)
                        .downloadElement();
        assertThat(collectionsDownloadElement, is(not(downloading())));
        assertThat("Playlist should be unavailable ", collectionsDownloadElement.isUnavailable());
    }

    public void testPlaylistsWithAvailableTracksAreNotMarkedAsUnavailable() {
        final PlaylistDetailsScreen playlistDetailsScreen = mainNavHelper
                .goToCollections()
                .clickPlaylistsPreview()
                .scrollToAndClickPlaylistWithTitle(MIXED_PLAYLIST)
                .clickDownloadToggle();

        final DownloadImageViewElement downloadElement = playlistDetailsScreen.headerDownloadElement();
        assertThat("Playlist should not be unavailable ", !downloadElement.isUnavailable());

        final DownloadImageViewElement collectionsDownloadElement = playlistDetailsScreen
                .goBackToPlaylists()
                .scrollToPlaylistWithTitle(MIXED_PLAYLIST)
                .downloadElement();
        assertThat("Playlist should not be unavailable ", !collectionsDownloadElement.isUnavailable());
    }

    public void testPlaylistIsRequestedWhenNetworkIsOff() {
        final PlaylistsScreen playlistsScreen = mainNavHelper.goToCollections().clickPlaylistsPreview();
        playlistsScreen.scrollToPlaylistWithTitle(OFFLINE_PLAYLIST);
        networkManagerClient.switchWifiOff();
        final PlaylistDetailsScreen playlistDetailsScreen = playlistsScreen
                .scrollToAndClickPlaylistWithTitle(OFFLINE_PLAYLIST)
                .clickDownloadToggle();

        final DownloadImageViewElement downloadElement = playlistDetailsScreen.headerDownloadElement();
        assertThat(downloadElement, is(not(downloading())));
        assertThat("Playlist should be requested ", downloadElement.isRequested());

        final DownloadImageViewElement collectionsDownloadElement = playlistDetailsScreen
                .goBackToPlaylists()
                .scrollToPlaylistWithTitle(OFFLINE_PLAYLIST)
                .downloadElement();
        assertThat(collectionsDownloadElement, is(not(downloading())));
        assertThat("Playlist should be requested ", collectionsDownloadElement.isRequested());
    }
}
