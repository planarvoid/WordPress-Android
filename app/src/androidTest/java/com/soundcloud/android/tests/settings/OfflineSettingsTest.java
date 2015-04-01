package com.soundcloud.android.tests.settings;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.OfflineSettingsScreen;
import com.soundcloud.android.screens.SettingsScreen;
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
        settingsScreen = new MainScreen(solo).actionBar().clickSettingsOverflowButton();
        offlineSettingsScreen = settingsScreen.clickOfflineSettings();
    }

    public void testOfflineLimitSlider() {
        assertTrue(offlineSettingsScreen.isVisible());
        assertEquals("1.0 GB", offlineSettingsScreen.getSliderLimitText());
        assertEquals("1.0 GB", offlineSettingsScreen.getLegendLimitText());

        offlineSettingsScreen.tapOnSlider(0);
        assertEquals("0.0 GB", offlineSettingsScreen.getSliderLimitText());
        assertEquals("0.0 GB", offlineSettingsScreen.getLegendLimitText());

        offlineSettingsScreen.tapOnSlider(80);
        String sliderLastValue = offlineSettingsScreen.getSliderLimitText();
        String legendLastValue = offlineSettingsScreen.getLegendLimitText();
        assertNotSame("0.0 GB", sliderLastValue);
        assertNotSame("0.0 GB", legendLastValue);

        solo.goBack();
        settingsScreen.clickOfflineSettings();
        assertEquals(sliderLastValue, offlineSettingsScreen.getSliderLimitText());
        assertEquals(legendLastValue, offlineSettingsScreen.getLegendLimitText());
    }
}
