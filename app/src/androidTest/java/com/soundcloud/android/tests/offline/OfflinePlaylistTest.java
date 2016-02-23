package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetOfflineSyncState;
import static com.soundcloud.android.screens.elements.DownloadImageViewElement.IsDownloading.downloading;
import static com.soundcloud.android.screens.elements.DownloadImageViewElement.IsDownloadingOrDownloaded.downloadingOrDownloaded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.elements.DownloadImageViewElement;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

public class OfflinePlaylistTest extends ActivityTest<MainActivity> {

    private static final String UNAVAILABLE_PLAYLIST = "Unavailable playlist";
    private static final String EMPTY_PLAYLIST = "Empty playlist";
    private static final String MIXED_PLAYLIST = "Mixed playlist";
    private static final String OFFLINE_PLAYLIST = "Offline playlist";

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
        final CollectionScreen collectionScreen = mainNavHelper
                .goToCollections();
        collectionScreen
                .scrollToPlaylistWithTitle("Offline playlist")
                .clickOverflow()
                .clickMakeAvailableOffline();

        PlaylistDetailsScreen playlistDetailsScreen =
                collectionScreen.scrollToPlaylistWithTitle("Offline playlist").click();

        assertThat(playlistDetailsScreen.headerDownloadElement(), is(downloadingOrDownloaded()));
    }

    public void testDownloadsPlaylistWhenMadeAvailableOfflineFromDetails() {
        final CollectionScreen collectionScreen = mainNavHelper
                .goToCollections();

        final PlaylistDetailsScreen playlistDetailsScreen = collectionScreen
                .scrollToAndClickPlaylistWithTitle(OFFLINE_PLAYLIST)
                .clickDownloadToggle();

        assertThat(playlistDetailsScreen.headerDownloadElement(), is(downloadingOrDownloaded()));

        final DownloadImageViewElement collectionsDownloadElement = playlistDetailsScreen.goBackToCollections().getPlaylistWithTitle(OFFLINE_PLAYLIST).downloadElement();
        assertThat(collectionsDownloadElement,  is(downloadingOrDownloaded()));
    }

    public void testDownloadPlaylistWhenMadeAvailableOfflineFromPlaylistDetails() {
        final PlaylistDetailsScreen playlistDetailsScreen = mainNavHelper.goToCollections()
                .scrollToAndClickPlaylistWithTitle(OFFLINE_PLAYLIST)
                .clickDownloadToggle();

        final DownloadImageViewElement downloadElement = playlistDetailsScreen.headerDownloadElement();
        assertThat(downloadElement, is(downloading()));

        final DownloadImageViewElement collectionsDownloadElement = playlistDetailsScreen.goBackToCollections().getPlaylistWithTitle(OFFLINE_PLAYLIST).downloadElement();
        assertThat(collectionsDownloadElement,  is(downloadingOrDownloaded()));
    }

    public void testEmptyPlaylistsAreMarkedAsRequested() throws Exception {
        final PlaylistDetailsScreen playlistDetailsScreen = mainNavHelper.goToCollections()
                .scrollToAndClickPlaylistWithTitle(EMPTY_PLAYLIST)
                .clickDownloadToggle();

        final DownloadImageViewElement downloadElement = playlistDetailsScreen.headerDownloadElement();
        assertThat(downloadElement, is(not(downloading())));
        assertThat("Playlist should be requested ", downloadElement.isRequested());

        final DownloadImageViewElement collectionsDownloadElement = playlistDetailsScreen.goBackToCollections().getPlaylistWithTitle(EMPTY_PLAYLIST).downloadElement();
        assertThat(collectionsDownloadElement, is(not(downloading())));
        assertThat("Playlist should be requested ", collectionsDownloadElement.isRequested());
    }

    public void testUnavailablePlaylistsAreMarkedAsUnavailable() throws Exception {
        final PlaylistDetailsScreen playlistDetailsScreen = mainNavHelper.goToCollections()
                .scrollToAndClickPlaylistWithTitle(UNAVAILABLE_PLAYLIST)
                .clickDownloadToggle();

        final DownloadImageViewElement downloadElement = playlistDetailsScreen.headerDownloadElement();
        assertThat(downloadElement, is(not(downloading())));
        assertThat("Playlist should be unavailable ", downloadElement.isUnavailable());

        final DownloadImageViewElement collectionsDownloadElement = playlistDetailsScreen.goBackToCollections().getPlaylistWithTitle(UNAVAILABLE_PLAYLIST).downloadElement();
        assertThat(collectionsDownloadElement, is(not(downloading())));
        assertThat("Playlist should be unavailable ", collectionsDownloadElement.isUnavailable());
    }

    public void testPlaylistsWithAvailableTracksAreNotMarkedAsUnavailable() throws Exception {
        final PlaylistDetailsScreen playlistDetailsScreen = mainNavHelper.goToCollections()
                .scrollToAndClickPlaylistWithTitle(MIXED_PLAYLIST)
                .clickDownloadToggle();

        final DownloadImageViewElement downloadElement = playlistDetailsScreen.headerDownloadElement();
        assertThat("Playlist should not be unavailable ", !downloadElement.isUnavailable());

        final DownloadImageViewElement collectionsDownloadElement = playlistDetailsScreen.goBackToCollections().getPlaylistWithTitle(MIXED_PLAYLIST).downloadElement();
        assertThat("Playlist should not be unavailable ", !collectionsDownloadElement.isUnavailable());
    }

    public void testPlaylistIsRequestedWhenNetworkIsOff() throws Exception {
        final CollectionScreen collectionScreen = mainNavHelper.goToCollections();
        collectionScreen.scrollToPlaylistWithTitle(OFFLINE_PLAYLIST);
        networkManagerClient.switchWifiOff();
        final PlaylistDetailsScreen playlistDetailsScreen = collectionScreen
                .scrollToAndClickPlaylistWithTitle(OFFLINE_PLAYLIST)
                .clickDownloadToggle();

        final DownloadImageViewElement downloadElement = playlistDetailsScreen.headerDownloadElement();
        assertThat(downloadElement, is(not(downloading())));
        assertThat("Playlist should be requested ", downloadElement.isRequested());


        final DownloadImageViewElement collectionsDownloadElement = playlistDetailsScreen.goBackToCollections().getPlaylistWithTitle(OFFLINE_PLAYLIST).downloadElement();
        assertThat(collectionsDownloadElement, is(not(downloading())));
        assertThat("Playlist should be requested ", collectionsDownloadElement.isRequested());
    }
}
