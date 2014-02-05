package com.soundcloud.android.preferences;

import android.os.Bundle;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;

public class AccountSettingsActivity extends ScSettingsActivity {

     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         addPreferencesFromResource(R.xml.account_settings);
     }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            mEventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SETTINGS_ACCOUNT.get());
        }
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}
