package com.soundcloud.android.tests.offline;

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
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.tests.SoundCloudTestApplication;
import org.junit.Test;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

public class OfflineQuotaTest extends ActivityTest<MainActivity> {

    private Context context;

    private final OfflineContentHelper offlineContentHelper;
    private final OfflineSettingsStorage offlineSettingsStorage;

    public OfflineQuotaTest() {
        super(MainActivity.class);
        offlineContentHelper = new OfflineContentHelper();
        offlineSettingsStorage = SoundCloudTestApplication.fromContext(InstrumentationRegistry.getTargetContext()).getOfflineSettingsStorage();
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
        // Allow downloading on mobile network as Firebase emulators don't have WiFi
        offlineSettingsStorage.setWifiOnlyEnabled(false);
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
        assertThat(trackLikesScreen.tracks().get(0).visibleDownloadElement(), is(requested()));
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        offlineContentHelper.clearOfflineContent(context);
    }
}
