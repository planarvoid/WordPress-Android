package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetOfflineSyncState;

import com.soundcloud.android.framework.IntegrationTestsFixtures;
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
    protected TestUser getUserForLogin() {
        return TestUser.offlineEmptyUser;
    }

    @Override
    public void setUp() throws Exception {
        final Context context = getInstrumentation().getTargetContext();

        IntegrationTestsFixtures testsFixtures = new IntegrationTestsFixtures();
        testsFixtures.clearLikes(context);
        super.setUp();

        resetOfflineSyncState(context);
        enableOfflineContent(context);
    }

    public void testDownloadsTrackWhenLiked() {
        mainNavHelper.goToStream()
                     .clickFirstTrackCardOverflowButton()
                     .toggleLike();

        final TrackLikesScreen likesScreen = mainNavHelper.goToTrackLikes();
        likesScreen
                .toggleOfflineEnabled()
                .clickKeepLikesSynced();

        final DownloadImageViewElement downloadElement = likesScreen
                .tracks()
                .get(0)
                .downloadElement();

        assertTrue(downloadElement.isRequested() || downloadElement.isDownloading() || downloadElement.isDownloaded());
    }

    public void testDownloadResumesWhenConnectionBack() {
        mainNavHelper.goToStream()
                     .clickFirstTrackCardOverflowButton()
                     .toggleLike();

        final TrackLikesScreen likesScreen = mainNavHelper.goToTrackLikes();

        networkManagerClient.switchWifiOff();

        final DownloadImageViewElement downloadElement = likesScreen
                .toggleOfflineEnabled()
                .clickKeepLikesSynced()
                .tracks()
                .get(0)
                .downloadElement();

        // we tried to download but it failed with connection error so its back to requested
        assertTrue("Track should be requested", downloadElement.isRequested());

        networkManagerClient.switchWifiOn();

        likesScreen.waitForLikesToStartDownloading();
        assertTrue("Track should be downloading", downloadElement.isDownloading() || downloadElement.isDownloaded());
    }

}
