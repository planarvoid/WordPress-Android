package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.disableOfflineSettingsOnboarding;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetOfflineSyncState;
import static com.soundcloud.android.screens.elements.DownloadImageViewElement.IsDownloading.downloading;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.elements.DownloadImageViewElement;

import android.content.Context;

import java.io.IOException;

public class OfflinePerformanceTrackingTest extends TrackingActivityTest<MainActivity> {
    private static final String OFFLINE_PLAYLIST_CANCEL_DOWNLOAD_TRACKING = "offline_playlist_cancel_download_tracking";
    private static final String OFFLINE_LIKES_STORAGE_LIMIT_TRACKING = "offline_likes_storage_limit_tracking";

    private Context context;
    private OfflineContentHelper offlineContentHelper;

    public OfflinePerformanceTrackingTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.offlineUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        context = getInstrumentation().getTargetContext();
        offlineContentHelper = new OfflineContentHelper();
        resetOfflineSyncState(context);
    }

    public void testCancelDownloadTracking() {
        enableOfflineContent(context);

        startEventTracking();

        final PlaylistDetailsScreen playlistDetailsScreen = mainNavHelper
                .goToCollections()
                .clickPlaylistsPreview()
                .scrollToAndClickPlaylistWithTitle("Offline tracking playlist")
                .clickDownloadToggle();

        DownloadImageViewElement downloadElement = playlistDetailsScreen.headerDownloadElement();
        assertThat(downloadElement, is(downloading()));

        assertThat(playlistDetailsScreen.clickDownloadToggle()
                                        .headerDownloadElement().isVisible(), is(false));

        finishEventTracking(OFFLINE_PLAYLIST_CANCEL_DOWNLOAD_TRACKING);
    }

    public void testStorageLimitErrorTracking() throws IOException {
        enableOfflineContent(context);
        disableOfflineSettingsOnboarding(context);
        offlineContentHelper.addFakeOfflineTrack(context, Urn.forTrack(123L), 530);

        // set minimum storage limit
        mainNavHelper.goToOfflineSettings().tapOnSlider(0);
        solo.goBack();

        startEventTracking();

        final TrackLikesScreen likesScreen = mainNavHelper
                .goToCollections()
                .clickLikedTracksPreview()
                .toggleOfflineEnabled()
                .clickKeepLikesSynced();

        DownloadImageViewElement downloadElement = likesScreen.headerDownloadElement();
        assertThat(downloadElement, is(not(downloading())));

        finishEventTracking(OFFLINE_LIKES_STORAGE_LIMIT_TRACKING);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        offlineContentHelper.clearOfflineContent(context);
    }

}
