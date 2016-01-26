package com.soundcloud.android.tests.collection;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.elements.PlaylistElement;
import com.soundcloud.android.tests.ActivityTest;

public class SyncEntireCollectionTest extends ActivityTest<LauncherActivity> {

    public SyncEntireCollectionTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.offlineUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        enableOfflineContent(getInstrumentation().getTargetContext());
    }

    private void enableSyncEntireCollection() {
        mainNavHelper.goToYou()
                .clickOfflineSettingsLink()
                .toggleSyncCollectionOn();
        solo.goBack();
    }

    public void testDisablingSyncEntireCollectionViaPlaylist() {
        enableSyncEntireCollection();
        final PlaylistElement playlist = mainNavHelper.goToCollections()
                .scrollToFirstPlaylist();

        playlist.clickOverflow()
                .clickMakeUnavailableOfflineToDisableSyncCollection()
                .clickOk();

        assertThat(playlist.downloadElement().isVisible(), is(false));
        assertThat(checkSyncEntireCollectionStatus(), is(false));
    }

    public void testCancelDisablingSyncEntireCollection() {
        enableSyncEntireCollection();
        final PlaylistElement playlist = mainNavHelper.goToCollections()
                .scrollToFirstPlaylist();

        playlist.clickOverflow()
                .clickMakeUnavailableOfflineToDisableSyncCollection()
                .clickCancel();

        assertThat(playlist.downloadElement().isVisible(), is(true));
        assertThat(checkSyncEntireCollectionStatus(), is(true));
    }

    public void testDisablingSyncEntireCollectionViaPlaylistLeavesOtherContentDownloaded() {
        enableSyncEntireCollection();
        final CollectionScreen screen = mainNavHelper.goToCollections();
        final PlaylistElement playlist1 = screen.scrollToFirstPlaylist();

        playlist1.clickOverflow()
                .clickMakeUnavailableOfflineToDisableSyncCollection()
                .clickOk();

        assertThat(playlist1.downloadElement().isVisible(), is(false));

        final PlaylistElement playlist2 = screen.scrollToPlaylistWithTitle("Offline playlist");

        assertThat(playlist2.downloadElement().isVisible(), is(true));
    }

    private boolean checkSyncEntireCollectionStatus() {
        return mainNavHelper.goToYou()
                .clickOfflineSettingsLink()
                .isOfflineCollectionChecked();
    }

}
