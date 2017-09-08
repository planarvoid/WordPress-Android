package com.soundcloud.android.tests.offline;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.soundcloud.android.framework.TestUser.offlineUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.disableOfflineSettingsOnboarding;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.elements.PlaylistElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

import android.content.Context;

public class SyncEntireCollectionTest extends ActivityTest<LauncherActivity> {

    public SyncEntireCollectionTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return offlineUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Context context = getInstrumentation().getTargetContext();
        enableOfflineContent(context);
        disableOfflineSettingsOnboarding(context);
    }

    private void enableSyncEntireCollection() {
        mainNavHelper.goToMore()
                     .clickOfflineSettingsLink()
                     .toggleSyncCollectionOn();
        solo.goBack();
    }

    @Test
    public void testDisablingSyncEntireCollectionViaPlaylistItem() throws Exception {
        enableSyncEntireCollection();

        final PlaylistsScreen playlistsScreen = mainNavHelper.goToCollections().clickPlaylistsPreview();
        final PlaylistElement playlist = playlistsScreen.scrollToFirstPlaylist();

        playlist.clickOverflow()
                .clickMakeUnavailableOfflineToDisableSyncCollection()
                .clickOk();

        assertThat(playlist.downloadElement().isVisible(), is(false));

        playlistsScreen.goBackToCollections();

        assertThat(checkSyncEntireCollectionStatus(), is(false));
    }

    @Test
    public void testDisablingSyncEntireCollectionViaPlaylistDetails() throws Exception {
        enableSyncEntireCollection();

        final PlaylistsScreen playlistsScreen = mainNavHelper.goToCollections().clickPlaylistsPreview();

        final PlaylistElement playlist = playlistsScreen
                .scrollToFirstPlaylist()
                .click()
                .clickDownloadToDisableSyncCollection()
                .clickOkForPlaylistDetails()
                .goBackToPlaylists()
                .scrollToFirstPlaylist();

        assertThat(playlist.downloadElement().isVisible(), is(false));

        playlistsScreen.goBackToCollections();

        assertThat(checkSyncEntireCollectionStatus(), is(false));
    }

    @Test
    public void testCancelDisablingSyncEntireCollection() throws Exception {
        enableSyncEntireCollection();

        final PlaylistsScreen playlistsScreen = mainNavHelper.goToCollections().clickPlaylistsPreview();

        final PlaylistElement playlist = playlistsScreen
                .scrollToFirstPlaylist()
                .click()
                .clickDownloadToDisableSyncCollection()
                .clickCancelForPlaylistDetails()
                .goBackToPlaylists()
                .scrollToFirstPlaylist();

        assertThat(playlist.downloadElement().isVisible(), is(true));

        playlistsScreen.goBackToCollections();

        assertThat(checkSyncEntireCollectionStatus(), is(true));
    }

    @Test
    public void testDisablingSyncEntireCollectionViaPlaylistLeavesOtherContentDownloaded() throws Exception {
        enableSyncEntireCollection();
        final PlaylistsScreen playlistsScreen = mainNavHelper.goToCollections().clickPlaylistsPreview();

        final PlaylistElement playlist1 = playlistsScreen
                .scrollToFirstPlaylist()
                .click()
                .clickDownloadToDisableSyncCollection()
                .clickOkForPlaylistDetails()
                .goBackToPlaylists()
                .scrollToFirstPlaylist();

        assertThat(playlist1.downloadElement().isVisible(), is(false));

        final PlaylistElement playlist2 = playlistsScreen.scrollToPlaylistWithTitle("Offline playlist");

        assertThat(playlist2.downloadElement().isVisible(), is(true));
    }

    private boolean checkSyncEntireCollectionStatus() {
        return mainNavHelper.goToMore()
                            .clickOfflineSettingsLink()
                            .isOfflineCollectionChecked();
    }

}
