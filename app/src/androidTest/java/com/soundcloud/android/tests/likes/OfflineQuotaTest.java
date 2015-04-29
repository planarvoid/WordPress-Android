package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.disableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.OfflineContentHelper.clearOfflineContent;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

public class OfflineQuotaTest extends ActivityTest<MainActivity> {

    public OfflineQuotaTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.offlineUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Context context = getInstrumentation().getTargetContext();
        disableOfflineContent(context);
        clearOfflineContent(context);
        enableOfflineContent(context);
    }

    public void testOfflineStateRequestedWhenNotEnoughSpace() {
        new StreamScreen(solo)
                .actionBar()
                .clickSettingsOverflowButton()
                .clickOfflineSettings()
                .tapOnSlider(0);

        solo.goBack();
        solo.goBack();

        final TrackLikesScreen trackLikesScreen = menuScreen
                .open()
                .clickLikes()
                .clickListHeaderOverflowButton()
                .clickMakeAvailableOffline()
                .clickKeepLikesSynced();

        assertTrue(trackLikesScreen.headerDownloadElement().isRequested());
        assertTrue(trackLikesScreen.tracks().get(0).downloadElement().isUnavailable());
    }
}
