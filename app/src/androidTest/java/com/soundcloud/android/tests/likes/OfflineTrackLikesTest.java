package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.disableOfflineSync;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.OfflineContentHelper.clearOfflineContent;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.NavigationHelper;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.SyncYourLikesScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

public class OfflineTrackLikesTest extends ActivityTest<MainActivity> {

    private Context context;

    public OfflineTrackLikesTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.offlineUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        context = getInstrumentation().getTargetContext();
        resetOfflineSyncState(context);
    }

    public void testDownloadActionAvailableWhenUserSubscribed() {
        enableOfflineContent(context);

        final TrackLikesScreen likesScreen = NavigationHelper.openLikedTracks(new MenuScreen(solo), getWaiter());

        assertFalse(likesScreen.isdownloadIconVisible());
        assertTrue(likesScreen.actionBar().syncAction().isVisible());
    }

    public void testDownloadsTracksWhenEnabledOfflineLikes() {
        enableOfflineContent(context);

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
        disableOfflineSync(context);
        clearOfflineContent(context);
    }
}
