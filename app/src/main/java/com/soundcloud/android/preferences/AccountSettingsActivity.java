package com.soundcloud.android.preferences;

import com.soundcloud.android.R;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.Event;

import android.os.Bundle;

public class AccountSettingsActivity extends ScSettingsActivity {

     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         addPreferencesFromResource(R.xml.account_settings);
     }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            Event.SCREEN_ENTERED.publish(Screen.SETTINGS_ACCOUNT.get());
        }
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}
