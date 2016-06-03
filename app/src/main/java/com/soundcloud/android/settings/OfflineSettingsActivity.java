package com.soundcloud.android.settings;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;

import javax.inject.Inject;

public class OfflineSettingsActivity extends LoggedInActivity {

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject Navigator navigator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentFragment(OfflineSettingsFragment.create());
    }

    @Override
    public boolean onSupportNavigateUp() {
        navigator.openMore(this);
        return true;
    }

    @Override
    public Screen getScreen() {
        return Screen.SETTINGS_OFFLINE;
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setContainerLayout(this);
    }

}
