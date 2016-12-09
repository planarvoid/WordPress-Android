package com.soundcloud.android.tests.settings;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.disableOfflineSettingsOnboarding;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineSettingsOnboarding;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.OfflineSyncTest;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.screens.OfflineSettingsOnboardingScreen;
import com.soundcloud.android.screens.OfflineSettingsScreen;
import com.soundcloud.android.screens.YouScreen;
import com.soundcloud.android.screens.elements.DownloadImageViewElement;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

import java.io.IOException;

public class OfflineSettingsTest extends ActivityTest<LauncherActivity> {
    private YouScreen youScreen;
    private OfflineSettingsScreen offlineSettingsScreen;
    private Context context;

    private final OfflineContentHelper offlineContentHelper;

    public OfflineSettingsTest() {
        super(LauncherActivity.class);
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
        enableOfflineContent(context);
        disableOfflineSettingsOnboarding(context);

        youScreen = mainNavHelper.goToYou();
    }

    @OfflineSyncTest
    public void testDisableSyncCollectionIsCancellable() {
        offlineSettingsScreen = youScreen.clickOfflineSettingsLink();
        assertTrue(offlineSettingsScreen.isVisible());

        OfflineSettingsScreen screen = offlineSettingsScreen.toggleSyncCollectionOn();
        screen.toggleSyncCollectionOff().clickCancelForPlaylistDetails();

        assertThat(screen.isOfflineCollectionChecked(), is(true));
    }

    @OfflineSyncTest
    public void testEnableSyncCollectionTriggersSync() {
        offlineSettingsScreen = youScreen.clickOfflineSettingsLink();
        assertTrue(offlineSettingsScreen.isVisible());

        offlineSettingsScreen.toggleSyncCollectionOn();
        assertTrue("Sync Offline Collections should be checked", offlineSettingsScreen.isOfflineCollectionChecked());

        getSolo().goBack();
        final DownloadImageViewElement downloadElement = mainNavHelper.goToCollections()
                                                                      .clickPlaylistsPreview()
                                                                      .scrollToFirstPlaylist()
                                                                      .downloadElement();

        assertThat(downloadElement.isVisible(), is(true));
    }

    @OfflineSyncTest
    public void testRemoveOfflineContentDisablesOfflineCollection() {
        offlineSettingsScreen = youScreen.clickOfflineSettingsLink();
        assertTrue(offlineSettingsScreen.isVisible());

        offlineSettingsScreen.toggleSyncCollectionOn()
                             .clickRemoveOfflineContent()
                             .clickConfirm();

        assertThat(offlineSettingsScreen.isOfflineCollectionChecked(), is(false));
    }

    public void testOfflineLimitSlider() {
        offlineSettingsScreen = youScreen.clickOfflineSettingsLink();
        assertTrue(offlineSettingsScreen.isVisible());

        assertEquals("unlimited", offlineSettingsScreen.getSliderLimitText());

        offlineSettingsScreen.tapOnSlider(0); // minimum is 0.5
        assertEquals("0.5 GB", offlineSettingsScreen.getSliderLimitText());
        assertEquals("0.5 GB", offlineSettingsScreen.getLegendLimitText());

        offlineSettingsScreen.tapOnSlider(80);
        String sliderLastValue = offlineSettingsScreen.getSliderLimitText();
        String legendLastValue = offlineSettingsScreen.getLegendLimitText();
        assertNotSame("0.5 GB", sliderLastValue);
        assertNotSame("0.5 GB", legendLastValue);

        solo.goBack();
        youScreen.clickOfflineSettingsLink();
        assertEquals(sliderLastValue, offlineSettingsScreen.getSliderLimitText());
        assertEquals(legendLastValue, offlineSettingsScreen.getLegendLimitText());
    }

    public void testBlockOfflineLimitSliderBelowCurrentUsage() throws IOException {
        offlineContentHelper.addFakeOfflineTrack(context, Urn.forTrack(123L), 800);

        offlineSettingsScreen = youScreen.clickOfflineSettingsLink();
        offlineSettingsScreen.tapOnSlider(0);

        assertTrue(waiter.expectToastWithText(toastObserver,
                                              solo.getString(R.string.offline_cannot_set_limit_below_usage)));
        assertEquals("0.8 GB", offlineSettingsScreen.getSliderLimitText());
        assertEquals("0.8 GB", offlineSettingsScreen.getLegendLimitText());
    }

    public void testOfflineSettingsOnboarding() {
        enableOfflineSettingsOnboarding(context);
        youScreen.clickOfflineSettingsLink();

        OfflineSettingsOnboardingScreen offlineSettingsOnboardingScreen = new OfflineSettingsOnboardingScreen(solo);
        assertTrue(offlineSettingsOnboardingScreen.isVisible());

        offlineSettingsScreen = offlineSettingsOnboardingScreen.clickContinue();
        assertTrue(offlineSettingsScreen.isVisible());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        offlineContentHelper.clearOfflineContent(context);
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
