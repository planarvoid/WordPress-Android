package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.disableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.OfflineContentHelper.clearOfflineContent;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.MainActivity;
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

        TrackLikesScreen trackLikesScreen =
                menuScreen
                        .open()
                        .clickLikes();

        assertFalse(trackLikesScreen.headerDownloadElement().isVisible());
        assertTrue(trackLikesScreen.clickListHeaderOverflowButton().isVisible());
    }

    public void testDownloadsTracksWhenEnabledOfflineLikes() {
        enableOfflineContent(context);

        final TrackLikesScreen likesScreen =
                menuScreen
                        .open()
                        .clickLikes()
                        .clickListHeaderOverflowButton()
                        .clickMakeAvailableOffline()
                        .clickKeepLikesSynced();

        assertTrue(likesScreen.isDownloadInProgressTextVisible());

        likesScreen.waitForLikesDownloadToFinish();

        assertEquals(OfflineContentHelper.offlineFilesCount(), likesScreen.getLoadedTrackCount());
        assertTrue(likesScreen.isLikedTracksTextVisible());
    }

    public void testShuffleLikesWhenOfflineWithNoTracksDownloaded() {
        enableOfflineContent(context);

        final TrackLikesScreen likesScreen = menuScreen.open().clickLikes();
        networkManager.switchWifiOff();

        likesScreen.clickShuffleButton();

        final String message = solo.getString(R.string.playback_missing_playable_tracks);
        assertTrue(waiter.expectToastWithText(toastObserver, message));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }

    private void resetOfflineSyncState(Context context) {
        disableOfflineContent(context);
        clearOfflineContent(context);
    }
}
