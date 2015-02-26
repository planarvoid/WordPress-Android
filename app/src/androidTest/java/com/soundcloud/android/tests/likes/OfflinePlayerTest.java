package com.soundcloud.android.tests.likes;

import android.content.Context;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.NavigationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineSync;
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
        enableOfflineSync(getActivity());
    }

    public void testTracksShouldPlayOffline() throws Exception {
        likesScreen = NavigationHelper.openLikedTracks(new MenuScreen(solo), getWaiter());
        likesScreen.actionBar().clickSyncLikesButton().clickKeepLikesSynced();
        likesScreen.waitForLikesSyncToFinish();
        networkManager.switchWifiOff();

        assertTrue(likesScreen.clickTrack(0).isExpendedPlayerPlaying());
    }

}
