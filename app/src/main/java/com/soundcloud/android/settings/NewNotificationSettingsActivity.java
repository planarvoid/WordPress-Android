package com.soundcloud.android.settings;

import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;

import javax.inject.Inject;

public class NewNotificationSettingsActivity extends LoggedInActivity {

    @Inject BaseLayoutHelper baseLayoutHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentFragment(NewNotificationSettingsFragment.create());
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    public Screen getScreen() {
        return Screen.SETTINGS_NOTIFICATIONS;
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setContainerLayout(this);
    }

}
