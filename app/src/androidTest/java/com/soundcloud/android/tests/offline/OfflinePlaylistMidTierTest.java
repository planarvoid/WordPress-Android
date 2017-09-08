package com.soundcloud.android.tests.offline;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.soundcloud.android.framework.TestUser.offlineUserMT;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetOfflineSyncState;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.screens.elements.OfflineStateButtonElement.IsDownloadingOrDownloaded.downloadingOrDownloadedState;
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
import org.junit.Test;

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
        return offlineUserMT;
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

    @Test
    public void testPreviewContentHasButtonToOpenUpgradeScreen() throws Exception {
        PlaylistDetailsScreen playlistDetailsScreen = downloadPlaylistAndSwitchWifiOff();

        VisualPlayerElement visualPlayerElement = playlistDetailsScreen.clickFirstTrack();
        UpgradeScreen upgradeScreen = visualPlayerElement.clickUpgrade();
        assertThat(upgradeScreen, is(visible()));
    }

    @Test
    public void testFreeContentIsPlayableAfterPlaylistIsMadeAvailableOffline() throws Exception {
        PlaylistDetailsScreen playlistDetailsScreen = downloadPlaylistAndSwitchWifiOff();

        final VisualPlayerElement lastTrack = playlistDetailsScreen.clickTrack(1);
        lastTrack.waitForExpandedPlayerToStartPlaying();
        assertThat(lastTrack.getTrackTitle(), is(equalTo(TRACK_TITLE)));
    }

    private PlaylistDetailsScreen downloadPlaylistAndSwitchWifiOff() {
        PlaylistDetailsScreen playlistDetailsScreen = playlistsScreen
                .scrollToPlaylistWithTitle(MIXED_PLAYLIST)
                .click()
                .clickDownloadButton();

        assertThat(playlistDetailsScreen.offlineButtonElement(), is(downloadingOrDownloadedState()));

        playlistDetailsScreen.scrollToBottom();
        playlistDetailsScreen.waitForDownloadToFinish();
        connectionHelper.setWifiConnected(false);

        assertThat("Playlist should be downloaded", playlistDetailsScreen.offlineButtonElement().isDownloadedState());

        return playlistDetailsScreen;
    }
}
