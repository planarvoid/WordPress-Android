package com.soundcloud.android.preferences;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.Event;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;

import android.os.Bundle;

@Tracking(page = Page.Settings_notifications)
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
        ((SoundCloudApplication)getApplication()).track(getClass());
        if (!isConfigurationChange() || isReallyResuming()) {
            Event.SCREEN_ENTERED.publish(Screen.SETTINGS_ACCOUNT.get());
        }
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}
