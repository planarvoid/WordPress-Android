package com.soundcloud.android.activity.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;

@Tracking(page = Page.Settings_notifications)
public class AccountSettings extends PreferenceActivity {
     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);

         addPreferencesFromResource(R.xml.account_settings);
     }

    @Override
    protected void onResume() {
        super.onResume();
        ((SoundCloudApplication)getApplication()).track(getClass());
    }
}
