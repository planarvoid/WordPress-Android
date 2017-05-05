package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.screens.elements.DownloadImageViewElement.IsDownloading.downloading;
import static com.soundcloud.android.screens.elements.OfflineStateButtonElement.IsDownloading.downloadingState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

public class OfflinePlayerTest extends ActivityTest<MainActivity> {

    private TrackLikesScreen likesScreen;
    private final OfflineContentHelper offlineContentHelper;

    public OfflinePlayerTest() {
        super(MainActivity.class);
        offlineContentHelper = new OfflineContentHelper();
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.offlineUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        Context context = getInstrumentation().getTargetContext();
        offlineContentHelper.clearOfflineContent(context);
        enableOfflineContent(context);
        getWaiter().waitForContentAndRetryIfLoadingFailed();
        likesScreen = mainNavHelper.goToTrackLikes();
    }

    public void testPlayOfflineTracksOnlyWhenContentDownloaded() throws Exception {
        likesScreen
                .toggleOfflineEnabled()
                .clickKeepLikesSyncedAndWaitToFinish();

        if (getFeatureFlags().isEnabled(Flag.NEW_OFFLINE_ICONS)) {
            assertThat(likesScreen.offlineButtonElement(), is(not(downloadingState())));
            assertThat("Likes should be downloaded", likesScreen.offlineButtonElement().isDownloadedState());
        } else {
            assertThat(likesScreen.headerDownloadElement(), is(not(downloading())));
            assertThat("Likes should be downloaded", likesScreen.headerDownloadElement().isDownloaded());
        }

        connectionHelper.setNetworkConnected(false);

        String nextOfflineTrack = likesScreen.getTrackTitle(2);

        final VisualPlayerElement visualPlayerElement = likesScreen.clickTrack(0);
        visualPlayerElement.waitForExpandedPlayerToStartPlaying();
        visualPlayerElement.waitForTheExpandedPlayerToPlayNextTrack();
        assertThat(visualPlayerElement.getTrackTitle(), is(equalTo(nextOfflineTrack)));

    }

    // Can be enabled again once the player itself makes use of the {@link ConnectionHelper}, otherwise it will not recognize that it should be offline
    @Ignore
    public void testShowErrorWhenContentNotDownloaded() throws Exception {
        connectionHelper.setNetworkConnected(false);
        final VisualPlayerElement visualPlayerElement = likesScreen.clickTrack(3);

        final String expectedError = solo.getString(R.string.playback_error);
        assertThat(visualPlayerElement.error(), is(equalTo(expectedError)));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
