package com.soundcloud.android.tests.settings;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.soundcloud.android.R.string;
import static com.soundcloud.android.R.string.offline_cannot_set_limit_below_usage;
import static com.soundcloud.android.framework.TestUser.offlineUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.disableOfflineSettingsOnboarding;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineSettingsOnboarding;
import static com.soundcloud.android.model.Urn.forTrack;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.screens.MoreScreen;
import com.soundcloud.android.screens.OfflineSettingsOnboardingScreen;
import com.soundcloud.android.screens.OfflineSettingsScreen;
import com.soundcloud.android.screens.elements.DownloadImageViewElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

import android.content.Context;

import java.io.IOException;

public class OfflineSettingsTest extends ActivityTest<LauncherActivity> {
    private MoreScreen moreScreen;
    private OfflineSettingsScreen offlineSettingsScreen;
    private Context context;

    private final OfflineContentHelper offlineContentHelper;

    public OfflineSettingsTest() {
        super(LauncherActivity.class);
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
        enableOfflineContent(context);
        disableOfflineSettingsOnboarding(context);

        moreScreen = mainNavHelper.goToMore();
    }

    @Test
    public void testDisableSyncCollectionIsCancellable() throws Exception {
        offlineSettingsScreen = moreScreen.clickOfflineSettingsLink();
        assertTrue(offlineSettingsScreen.isVisible());

        OfflineSettingsScreen screen = offlineSettingsScreen.toggleSyncCollectionOn();
        screen.toggleSyncCollectionOff().clickCancelForPlaylistDetails();

        assertThat(screen.isOfflineCollectionChecked(), is(true));
    }

    @Test
    public void testEnableSyncCollectionTriggersSync() throws Exception {
        offlineSettingsScreen = moreScreen.clickOfflineSettingsLink();
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

    @Test
    public void testRemoveOfflineContentDisablesOfflineCollection() throws Exception {
        offlineSettingsScreen = moreScreen.clickOfflineSettingsLink();
        assertTrue(offlineSettingsScreen.isVisible());

        offlineSettingsScreen.toggleSyncCollectionOn()
                             .clickRemoveOfflineContent()
                             .clickConfirm();

        assertThat(offlineSettingsScreen.isOfflineCollectionChecked(), is(false));
    }

    @Test
    public void testOfflineLimitSlider() throws Exception {
        offlineSettingsScreen = moreScreen.clickOfflineSettingsLink();
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
        moreScreen.clickOfflineSettingsLink();
        assertEquals(sliderLastValue, offlineSettingsScreen.getSliderLimitText());
        assertEquals(legendLastValue, offlineSettingsScreen.getLegendLimitText());
    }

    @Test
    public void testBlockOfflineLimitSliderBelowCurrentUsage() throws Exception {
        offlineContentHelper.addFakeOfflineTrack(context, forTrack(123L), 800);

        offlineSettingsScreen = moreScreen.clickOfflineSettingsLink();
        offlineSettingsScreen.tapOnSlider(0);

        assertTrue(waiter.expectToastWithText(toastObserver,
                                              solo.getString(offline_cannot_set_limit_below_usage)));
        assertEquals("0.8 GB", offlineSettingsScreen.getSliderLimitText());
        assertEquals("0.8 GB", offlineSettingsScreen.getLegendLimitText());
    }

    @Test
    public void testOfflineSettingsOnboarding() throws Exception {
        enableOfflineSettingsOnboarding(context);
        moreScreen.clickOfflineSettingsLink();

        OfflineSettingsOnboardingScreen offlineSettingsOnboardingScreen = new OfflineSettingsOnboardingScreen(solo);
        assertTrue(offlineSettingsOnboardingScreen.isVisible());

        offlineSettingsScreen = offlineSettingsOnboardingScreen.clickContinue();
        assertTrue(offlineSettingsScreen.isVisible());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        offlineContentHelper.clearOfflineContent(context);
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
