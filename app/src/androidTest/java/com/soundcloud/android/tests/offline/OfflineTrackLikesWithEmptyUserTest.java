package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.TestUser.offlineEmptyUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetOfflineSyncState;
import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.framework.IntegrationTestsFixtures;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.elements.DownloadImageViewElement;
import com.soundcloud.android.screens.elements.StreamCardElement;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

import android.content.Context;

public class OfflineTrackLikesWithEmptyUserTest extends ActivityTest<MainActivity> {

    public OfflineTrackLikesWithEmptyUserTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return offlineEmptyUser;
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

    @Test
    public void testDownloadsTrackWhenLiked() throws Exception {
        likeFirstTrack();

        final TrackLikesScreen likesScreen = mainNavHelper.goToTrackLikes();
        likesScreen
                .toggleOfflineEnabled()
                .clickKeepLikesSynced();

        final DownloadImageViewElement downloadElement = likesScreen
                .tracks()
                .get(0)
                .visibleDownloadElement();

        assertTrue(downloadElement.isRequested() || downloadElement.isDownloading() || downloadElement.isDownloaded());
    }

    @Test
    public void testDownloadResumesWhenConnectionBack() throws Exception {
        likeFirstTrack();

        final TrackLikesScreen likesScreen = mainNavHelper.goToTrackLikes();

        connectionHelper.setWifiConnected(false);

        final TrackItemElement trackItemElement = likesScreen
                .toggleOfflineEnabled()
                .clickKeepLikesSynced()
                .tracks()
                .get(0);

        assertTrue("No internet element should be visible", trackItemElement.noInternetElement().hasVisibility());
        
        DownloadImageViewElement downloadImageElement = trackItemElement.downloadElement();
        assertTrue("Download element should not be visible", !downloadImageElement.isVisible());

        connectionHelper.setWifiConnected(true);

        likesScreen.waitForLikesToStartDownloading();
        assertTrue("Download element should be visible", downloadImageElement.isVisible());
        assertTrue("Track should be downloading", downloadImageElement.isDownloading() || downloadImageElement.isDownloaded());
    }

    private void likeFirstTrack() {
        final StreamScreen streamScreen = mainNavHelper.goToStream();
        final StreamCardElement streamCardElement = streamScreen.scrollToFirstTrack();
        if (!streamCardElement.isLiked()) {
            streamScreen
                    .clickFirstTrackCardOverflowButton()
                    .toggleLike();
        }
    }

}
