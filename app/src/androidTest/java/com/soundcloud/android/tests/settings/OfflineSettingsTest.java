package com.soundcloud.android.tests.settings;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.screens.OfflineSettingsScreen;
import com.soundcloud.android.screens.SettingsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

import java.io.IOException;

public class OfflineSettingsTest extends ActivityTest<MainActivity> {
    private SettingsScreen settingsScreen;
    private OfflineSettingsScreen offlineSettingsScreen;
    private Context context;

    private final OfflineContentHelper offlineContentHelper;

    public OfflineSettingsTest() {
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
        enableOfflineContent(context);

        settingsScreen = new StreamScreen(solo).actionBar().clickSettingsOverflowButton();
    }

    public void testOfflineLimitSlider() {
        offlineSettingsScreen = settingsScreen.clickOfflineSettings();
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
        settingsScreen.clickOfflineSettings();
        assertEquals(sliderLastValue, offlineSettingsScreen.getSliderLimitText());
        assertEquals(legendLastValue, offlineSettingsScreen.getLegendLimitText());
    }

    public void testBlockOfflineLimitSliderBelowCurrentUsage() throws IOException {
        offlineContentHelper.addFakeOfflineTrack(context, Urn.forTrack(123L), 800);

        offlineSettingsScreen = settingsScreen.clickOfflineSettings();
        offlineSettingsScreen.tapOnSlider(0);

        assertTrue(waiter.expectToastWithText(toastObserver, solo.getString(R.string.offline_cannot_set_limit_below_usage)));
        assertEquals("0.8 GB", offlineSettingsScreen.getSliderLimitText());
        assertEquals("0.8 GB", offlineSettingsScreen.getLegendLimitText());
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
