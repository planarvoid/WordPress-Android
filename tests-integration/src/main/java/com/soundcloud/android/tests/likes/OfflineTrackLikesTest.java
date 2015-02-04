package com.soundcloud.android.tests.likes;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.NavigationHelper;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.LikesScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.SyncYourLikesScreen;
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

    @Override
    protected void tearDown() throws Exception {
        resetOfflineSyncState(getInstrumentation().getTargetContext());
        super.tearDown();
    }

    public void testTrackLikesFragmentOfflineSyncIsEnabled() {
        ConfigurationHelper.enableOfflineSync(getActivity());

        final LikesScreen likesScreen = NavigationHelper.openLikedTracks(new MenuScreen(solo), getWaiter());

        assertFalse(likesScreen.isSyncIconVisible());
        assertTrue(likesScreen.actionBar().syncAction().isVisible());
    }

    public void testToggleOfflineSyncOfLikesDownloadsTracksOntoDevice() {
        ConfigurationHelper.enableOfflineSync(getActivity());

        final LikesScreen likesScreen = NavigationHelper.openLikedTracks(new MenuScreen(solo), getWaiter());
        final SyncYourLikesScreen syncLikesDialog = likesScreen.actionBar().clickSyncLikesButton();
        assertTrue(syncLikesDialog.isVisible());

        syncLikesDialog.clickKeepLikesSynced();
        assertTrue(likesScreen.isSyncInProgressTextVisible());

        likesScreen.waitForLikesSyncToFinish();
        assertEquals(OfflineContentHelper.offlineFilesCount(), likesScreen.getLoadedTrackCount());
        assertTrue(likesScreen.isLikedTracksTextVisible());
    }

    private void resetOfflineSyncState(Context context) {
        ConfigurationHelper.disableOfflineSync(context);
        OfflineContentHelper.clearOfflineContent(context);
    }
}
