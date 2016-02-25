package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.CollectionsTest;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

import java.io.IOException;

public class OfflineQuotaTest extends ActivityTest<MainActivity> {

    private final OfflineContentHelper offlineContentHelper;
    private Context context;

    public OfflineQuotaTest() {
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

        offlineContentHelper.clearOfflineContent(context);
        enableOfflineContent(context);
    }

    @CollectionsTest
    public void testOfflineStateRequestedWhenNotEnoughSpace() throws IOException {
        offlineContentHelper.addFakeOfflineTrack(context, Urn.forTrack(123L), 530);

        mainNavHelper.goToOfflineSettings().tapOnSlider(0);

        solo.goBack();

        final TrackLikesScreen trackLikesScreen = mainNavHelper
                .goToTrackLikes()
                .toggleOfflineEnabled()
                .clickKeepLikesSynced();

        assertTrue(trackLikesScreen.headerDownloadElement().isRequested());
        assertTrue(trackLikesScreen.tracks().get(0).downloadElement().isRequested());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        offlineContentHelper.clearOfflineContent(context);
    }
}
