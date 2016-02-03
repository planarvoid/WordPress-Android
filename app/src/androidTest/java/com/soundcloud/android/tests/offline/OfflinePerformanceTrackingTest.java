package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetOfflineSyncState;
import static com.soundcloud.android.screens.elements.DownloadImageViewElement.IsDownloading.downloading;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.elements.DownloadImageViewElement;

import android.content.Context;

@EventTrackingTest
public class OfflinePerformanceTrackingTest extends TrackingActivityTest<MainActivity> {
    private static final String OFFLINE_PLAYLIST_CANCEL_DOWNLOAD_TRACKING = "offline_playlist_cancel_download_tracking";

    private Context context;

    public OfflinePerformanceTrackingTest() {
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

    public void testCancelDownloadTracking() {
        enableOfflineContent(context);

        startEventTracking();

        final PlaylistDetailsScreen playlistDetailsScreen = mainNavHelper
                .goToCollections()
                .scrollToAndClickPlaylistWithTitle("Offline tracking playlist")
                .clickPlaylistOverflowButton()
                .clickMakeAvailableOffline();

        DownloadImageViewElement downloadElement = playlistDetailsScreen.headerDownloadElement();
        assertThat(downloadElement, is(downloading()));

        assertThat(playlistDetailsScreen.clickPlaylistOverflowButton()
                .clickMakeUnavailableOffline()
                .headerDownloadElement().isVisible(), is(false));

        finishEventTracking(OFFLINE_PLAYLIST_CANCEL_DOWNLOAD_TRACKING);
    }

}
