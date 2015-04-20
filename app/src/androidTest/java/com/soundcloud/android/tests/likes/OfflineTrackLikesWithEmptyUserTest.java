package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetOfflineSyncState;
import static com.soundcloud.android.framework.helpers.OfflineContentHelper.clearLikes;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.elements.DownloadImageViewElement;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

public class OfflineTrackLikesWithEmptyUserTest extends ActivityTest<MainActivity> {

    public OfflineTrackLikesWithEmptyUserTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.offlineEmptyUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        final Context context = getInstrumentation().getTargetContext();

        clearLikes(context);
        super.setUp();

        resetOfflineSyncState(context);
        enableOfflineContent(context);
    }

    public void testDownloadsTrackWhenLiked() {
        menuScreen
                .open()
                .clickStream()
                .clickFirstTrackOverflowButton()
                .toggleLike();

        final TrackLikesScreen likesScreen = menuScreen
                .open()
                .clickLikes();

        likesScreen
                .clickListHeaderOverflowButton()
                .clickMakeAvailableOffline()
                .clickKeepLikesSynced();

        final DownloadImageViewElement downloadElement = likesScreen
                .tracks()
                .get(0)
                .downloadElement();

        assertTrue(downloadElement.isRequested() || downloadElement.isDownloading() || downloadElement.isDownloaded());
    }

    public void testDownloadResumesWhenConnectionBack() {
        menuScreen
                .open()
                .clickStream()
                .clickFirstTrackOverflowButton()
                .toggleLike();

        final TrackLikesScreen likesScreen = menuScreen.open().clickLikes();

        networkManager.switchWifiOff();

        final DownloadImageViewElement downloadElement = likesScreen
                .clickListHeaderOverflowButton()
                .clickMakeAvailableOffline()
                .clickKeepLikesSynced()
                .tracks()
                .get(0)
                .downloadElement();

        // we tried to download but it failed with connection error so its not unavailable
        assertTrue("Track should be unavailable", downloadElement.isUnavailable());

        networkManager.switchWifiOn();

        likesScreen.waitForLikesToStartDownloading();
        assertTrue("Track should be downloading", downloadElement.isDownloading() || downloadElement.isDownloaded());
    }

}
