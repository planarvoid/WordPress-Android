package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.TestUser.emptyUser;
import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class EmptyTrackLikesTest extends ActivityTest<MainActivity> {

    public EmptyTrackLikesTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return emptyUser;
    }

    @Test
    public void testShowsEmptyLikesScreen() throws Exception {
        TrackLikesScreen likesScreen = mainNavHelper.goToTrackLikes();

        waiter.waitForContentAndRetryIfLoadingFailed();
        assertTrue(likesScreen.emptyView().isVisible());
    }
}
