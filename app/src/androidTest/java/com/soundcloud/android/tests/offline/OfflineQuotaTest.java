package com.soundcloud.android.tests.offline;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.soundcloud.android.framework.TestUser.offlineUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.disableOfflineSettingsOnboarding;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.model.Urn.forTrack;
import static com.soundcloud.android.screens.elements.DownloadImageViewElement.IsRequested.requested;
import static com.soundcloud.android.screens.elements.OfflineStateButtonElement.IsDefault.defaultState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

import android.content.Context;

import java.io.IOException;

public class OfflineQuotaTest extends ActivityTest<MainActivity> {

    private Context context;

    private final OfflineContentHelper offlineContentHelper;

    public OfflineQuotaTest() {
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

        offlineContentHelper.clearOfflineContent(context);
        enableOfflineContent(context);
        disableOfflineSettingsOnboarding(context);
    }

    @Test
    public void testOfflineStateRequestedWhenNotEnoughSpace() throws Exception {
        offlineContentHelper.addFakeOfflineTrack(context, forTrack(123L), 530);

        mainNavHelper.goToOfflineSettings().tapOnSlider(0);

        solo.goBack();

        final TrackLikesScreen trackLikesScreen = mainNavHelper
                .goToTrackLikes()
                .toggleOfflineEnabled()
                .clickKeepLikesSynced();

        assertThat(trackLikesScreen.offlineButtonElement(), is(defaultState()));
        assertThat(trackLikesScreen.tracks().get(0).downloadElement(), is(requested()));
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        offlineContentHelper.clearOfflineContent(context);
    }
}
