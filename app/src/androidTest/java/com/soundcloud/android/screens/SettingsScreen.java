package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.settings.SettingsActivity;

public class SettingsScreen extends Screen {

    private static final Class ACTIVITY = SettingsActivity.class;

    public SettingsScreen(Han solo) {
        super(solo);
    }

    public OfflineSettingsScreen clickOfflineSettings() {
        testDriver.clickOnText(R.string.pref_offline_settings);
        return new OfflineSettingsScreen(testDriver);
    }

    public HomeScreen clickLogoutAndConfirm() {
        logoutListItem().click();
        // TODO: This is bad! Do a dialog Screen element instead
        testDriver.clickOnText(android.R.string.ok);
        return new HomeScreen(testDriver);
    }

    private ViewElement logoutListItem() {
        return testDriver.findElement(With.text(testDriver.getString(R.string.pref_revoke_access)));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
