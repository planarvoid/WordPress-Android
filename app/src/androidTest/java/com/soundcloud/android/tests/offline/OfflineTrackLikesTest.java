package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetOfflineSyncState;
import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;


public class OfflineTrackLikesTest extends ActivityTest<MainActivity> {
    private static final String OFFLINE_LIKES_PERFORMANCE_TRACKING = "specs/offline_likes_performance_tracking.spec";

    private final OfflineContentHelper offlineContentHelper;
    private Context context;

    public OfflineTrackLikesTest() {
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

        context = getInstrumentation().getTargetContext();
        resetOfflineSyncState(context);
    }

    public void testOfflineSyncMenuAvailableWhenUserSubscribed() {
        enableOfflineContent(context);

        final TrackLikesScreen trackLikesScreen = mainNavHelper.goToTrackLikes();

        assertThat(trackLikesScreen.offlineToggle(), is(visible()));
    }

    public void testOfflineSyncOfOfflineLikes() throws Exception {
        enableOfflineContent(context);

        mrLocalLocal.startEventTracking();

        final TrackLikesScreen likesScreen = mainNavHelper
                .goToTrackLikes()
                .toggleOfflineEnabled()
                .clickKeepLikesSyncedAndWaitToFinish();

        // there is one creator opt out track liked
        assertTrue(likesScreen.isLikedTracksTextVisible());
        assertEquals(offlineContentHelper.offlineFilesCount(context), likesScreen.getTotalLikesCount() - 1);

        mrLocalLocal.verify(OFFLINE_LIKES_PERFORMANCE_TRACKING);
    }
}
