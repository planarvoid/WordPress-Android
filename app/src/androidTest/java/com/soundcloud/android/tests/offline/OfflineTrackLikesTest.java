package com.soundcloud.android.tests.offline;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.soundcloud.android.framework.TestUser.offlineUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetOfflineSyncState;
import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

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
        return offlineUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        context = getInstrumentation().getTargetContext();
        resetOfflineSyncState(context);
    }

    @Test
    public void testOfflineSyncMenuAvailableWhenUserSubscribed() throws Exception {
        enableOfflineContent(context);

        final TrackLikesScreen trackLikesScreen = mainNavHelper.goToTrackLikes();

        assertThat(trackLikesScreen.offlineButton(), is(visible()));
    }

    @Test
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
