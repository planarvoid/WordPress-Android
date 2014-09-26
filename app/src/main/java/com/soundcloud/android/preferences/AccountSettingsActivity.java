package com.soundcloud.android.preferences;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;

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
            eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SETTINGS_ACCOUNT.get());
        }
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}
