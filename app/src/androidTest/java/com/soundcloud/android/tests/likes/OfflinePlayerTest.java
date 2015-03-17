package com.soundcloud.android.tests.likes;

import android.content.Context;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.NavigationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.OfflineContentHelper.clearOfflineContent;

public class OfflinePlayerTest extends ActivityTest<MainActivity> {

    private TrackLikesScreen likesScreen;

    public OfflinePlayerTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        final Context context = getInstrumentation().getTargetContext();
        TestUser.offlineUser.logIn(context);

        super.setUp();

        clearOfflineContent(context);
        enableOfflineContent(getActivity());
        likesScreen = NavigationHelper.openLikedTracks(new MenuScreen(solo), getWaiter());
    }

    public void testPlayTrackWhenContentDownloaded() throws Exception {
        likesScreen.actionBar().clickSyncLikesButton().clickKeepLikesSynced();
        likesScreen.waitForLikesdownloadToFinish();
        networkManager.switchWifiOff();

        assertTrue(likesScreen.clickTrack(0).isExpendedPlayerPlaying());
    }

    public void testShowToastWhenContentNotDownloaded() throws Exception {
        networkManager.switchWifiOff();
        toastObserver.observe();
        likesScreen.clickTrack(0);
        toastObserver.stopObserving();

        assertTrue(toastObserver.wasToastObserved("Track is not available offline"));
    }

}
