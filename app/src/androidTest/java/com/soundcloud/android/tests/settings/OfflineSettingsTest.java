package com.soundcloud.android.tests.settings;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.OfflineSettingsScreen;
import com.soundcloud.android.screens.SettingsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.ActivityTest;

public class OfflineSettingsTest extends ActivityTest<MainActivity> {
    private SettingsScreen settingsScreen;
    private OfflineSettingsScreen offlineSettingsScreen;

    public OfflineSettingsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.offlineUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        enableOfflineContent(getInstrumentation().getTargetContext());
        settingsScreen = new StreamScreen(solo).actionBar().clickSettingsOverflowButton();
        offlineSettingsScreen = settingsScreen.clickOfflineSettings();
    }

    public void testOfflineLimitSlider() {
        assertTrue(offlineSettingsScreen.isVisible());
        assertEquals("1.0 GB", offlineSettingsScreen.getSliderLimitText());
        assertEquals("1.0 GB", offlineSettingsScreen.getLegendLimitText());

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
}
