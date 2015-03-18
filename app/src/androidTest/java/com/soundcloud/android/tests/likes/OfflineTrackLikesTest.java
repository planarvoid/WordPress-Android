package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.NavigationHelper;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.SyncYourLikesScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

public class OfflineTrackLikesTest extends ActivityTest<MainActivity> {

    public OfflineTrackLikesTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        final Context context = getInstrumentation().getTargetContext();

        resetOfflineSyncState(context);
        TestUser.offlineUser.logIn(context);
        super.setUp();
    }

    public void testDownloadActionAvailableWhenUserSubscribed() {
        enableOfflineContent(getActivity());

        final TrackLikesScreen likesScreen = NavigationHelper.openLikedTracks(new MenuScreen(solo), getWaiter());

        assertFalse(likesScreen.isdownloadIconVisible());
        assertTrue(likesScreen.actionBar().syncAction().isVisible());
    }

    public void testDownloadsTracksWhenEnabledOfflineLikes() {
        enableOfflineContent(getActivity());

        final TrackLikesScreen likesScreen = NavigationHelper.openLikedTracks(new MenuScreen(solo), getWaiter());
        final SyncYourLikesScreen syncLikesDialog = likesScreen.actionBar().clickSyncLikesButton();
        assertTrue(syncLikesDialog.isVisible());

        syncLikesDialog.clickKeepLikesSynced();
        assertTrue(likesScreen.isDownloadInProgressTextVisible());

        likesScreen.waitForLikesdownloadToFinish();
        assertEquals(OfflineContentHelper.offlineFilesCount(), likesScreen.getLoadedTrackCount());
        assertTrue(likesScreen.isLikedTracksTextVisible());
    }

    private void resetOfflineSyncState(Context context) {
        ConfigurationHelper.disableOfflineSync(context);
        OfflineContentHelper.clearOfflineContent(context);
    }
}
