package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.disableOfflineSync;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.OfflineContentHelper.clearOfflineContent;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MainScreen;
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
        disableOfflineSync(context);
        clearOfflineContent(context);
        enableOfflineContent(context);
    }

    public void testOfflineStateRequestedWhenNotEnoughSpace() {
        new MainScreen(solo)
                .actionBar()
                .clickSettingsOverflowButton()
                .clickOfflineSettings()
                .tapOnSlider(0);

        solo.goBack();
        solo.goBack();

        final TrackLikesScreen trackLikesScreen = menuScreen
                .open()
                .clickLikes()
                .actionBar()
                .clickSyncLikesButton()
                .clickKeepLikesSynced();

        assertTrue(trackLikesScreen.actionBar().downloadElement().isRequested());
        assertTrue(trackLikesScreen.tracks().get(0).downloadElement().isUnavailable());
    }
}
