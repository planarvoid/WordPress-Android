package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetOfflineSyncState;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.screens.elements.DownloadImageViewElement.IsDownloadingOrDownloaded.downloadingOrDownloaded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

public class OfflinePlaylistMidTierTest extends ActivityTest<MainActivity> {
    private static final String MIXED_PLAYLIST = "Mixed playlist";
    private static final String TRACK_TITLE = "Sounds from Wednesday morning";

    private PlaylistsScreen playlistsScreen;

    public OfflinePlaylistMidTierTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.offlineUserMT;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        final Context context = getInstrumentation().getTargetContext();
        resetOfflineSyncState(context);
        enableOfflineContent(context);
        getWaiter().waitForContentAndRetryIfLoadingFailed();
        playlistsScreen = mainNavHelper
                .goToCollections()
                .clickPlaylistsPreview();
    }

    public void testPreviewContentHasButtonToOpenUpgradeScreen() {
        PlaylistDetailsScreen playlistDetailsScreen = downloadPlaylistAndSwitchWifiOff();

        VisualPlayerElement visualPlayerElement = playlistDetailsScreen.clickFirstTrack();
        UpgradeScreen upgradeScreen = visualPlayerElement.clickUpgrade();
        assertThat(upgradeScreen, is(visible()));
    }

    public void testFreeContentIsPlayableAfterPlaylistIsMadeAvailableOffline() {
        PlaylistDetailsScreen playlistDetailsScreen = downloadPlaylistAndSwitchWifiOff();

        final VisualPlayerElement lastTrack = playlistDetailsScreen.clickTrack(1);
        lastTrack.waitForExpandedPlayerToStartPlaying();
        assertThat(lastTrack.getTrackTitle(), is(equalTo(TRACK_TITLE)));
    }

    private PlaylistDetailsScreen downloadPlaylistAndSwitchWifiOff() {
        PlaylistDetailsScreen playlistDetailsScreen = playlistsScreen
                .scrollToPlaylistWithTitle(MIXED_PLAYLIST)
                .click()
                .clickDownloadToggle();

        assertThat(playlistDetailsScreen.headerDownloadElement(), is(downloadingOrDownloaded()));

        playlistDetailsScreen.scrollToBottom();
        playlistDetailsScreen.waitForDownloadToFinish();
        networkManagerClient.switchWifiOff();

        assertThat("Playlist should be downloaded", playlistDetailsScreen.headerDownloadElement().isDownloaded());
        return playlistDetailsScreen;
    }
}
