package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetOfflineSyncState;
import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.CollectionsTest;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

public class OfflineTrackLikesTest extends ActivityTest<MainActivity> {

    private final OfflineContentHelper offlineContentHelper;
    private Context context;

    public OfflineTrackLikesTest() {
        super(MainActivity.class);
        offlineContentHelper = new OfflineContentHelper();
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

    @CollectionsTest
    public void testDownloadActionAvailableWhenUserSubscribed() {
        enableOfflineContent(context);

        final TrackLikesScreen trackLikesScreen = mainNavHelper.goToTrackLikes();

        assertFalse(trackLikesScreen.headerDownloadElement().isVisible());
        assertThat(trackLikesScreen.overflowButton(), is(visible()));
    }

    @CollectionsTest
    public void testDownloadsTracksWhenEnabledOfflineLikes() {
        enableOfflineContent(context);

        final TrackLikesScreen likesScreen = mainNavHelper
                .goToTrackLikes()
                .clickOverflowButton()
                .clickMakeAvailableOffline()
                .clickKeepLikesSyncedAndWaitToFinish();

        assertEquals(offlineContentHelper.offlineFilesCount(), likesScreen.getTotalLikesCount());
        assertTrue(likesScreen.isLikedTracksTextVisible());
    }

    @CollectionsTest
    public void testShuffleLikesWhenOfflineWithNoTracksDownloaded() {
        enableOfflineContent(context);

        final TrackLikesScreen likesScreen = mainNavHelper.goToTrackLikes();
        networkManagerClient.switchWifiOff();

        likesScreen.clickShuffleButton();

        final String message = solo.getString(R.string.playback_missing_playable_tracks);
        assertTrue(waiter.expectToastWithText(toastObserver, message));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
