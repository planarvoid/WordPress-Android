package com.soundcloud.android.preferences;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;

import android.os.Bundle;

public class AccountSettingsActivity extends ScSettingsActivity {

    public AccountSettingsActivity() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_account);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SETTINGS_ACCOUNT));
        }
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}
