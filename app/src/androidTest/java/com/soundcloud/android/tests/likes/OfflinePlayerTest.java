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
        likesScreen
                .clickListHeaderOverflowButton()
                .clickMakeAvailableOffline()
                .clickKeepLikesSyncedAndWaitToFinish();
        networkManager.switchWifiOff();

        assertTrue(likesScreen.clickTrack(0).isExpandedPlayerPlaying());
    }

    public void testShowToastWhenContentNotDownloaded() throws Exception {
        networkManager.switchWifiOff();
        likesScreen.clickTrack(0);

        assertTrue(waiter.expectToastWithText(toastObserver, "Track is not available offline"));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
