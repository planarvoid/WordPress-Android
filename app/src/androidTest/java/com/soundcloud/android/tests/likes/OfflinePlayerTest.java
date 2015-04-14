package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.OfflineContentHelper.clearOfflineContent;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

public class OfflinePlayerTest extends ActivityTest<MainActivity> {

    private TrackLikesScreen likesScreen;

    public OfflinePlayerTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.offlineUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        Context context = getInstrumentation().getTargetContext();
        clearOfflineContent(context);
        enableOfflineContent(context);
        getWaiter().waitForContentAndRetryIfLoadingFailed();
        likesScreen = menuScreen.open().clickLikes();
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
