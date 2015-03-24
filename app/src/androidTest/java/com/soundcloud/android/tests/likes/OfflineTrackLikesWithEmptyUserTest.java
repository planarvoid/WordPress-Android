package com.soundcloud.android.tests.likes;

import android.content.Context;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.ActivityTest;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.OfflineContentHelper.clearLikes;
import static com.soundcloud.android.framework.helpers.OfflineContentHelper.clearOfflineContent;

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

        clearOfflineContent(context);
        clearLikes(context);
        super.setUp();
        enableOfflineContent(getActivity());
    }

    public void testDownloadsTrackWhenLiked() {
        menuScreen
                .open()
                .clickLikes()
                .actionBar()
                .clickSyncLikesButton()
                .clickKeepLikesSynced();

        menuScreen
                .open()
                .clickStream()
                .clickFirstTrackOverflowButton()
                .toggleLike();

        assertTrue(menuScreen
                .open()
                .clickLikes()
                .tracks()
                .get(0)
                .isDownloadingOrDownloaded()
        );
    }

}
