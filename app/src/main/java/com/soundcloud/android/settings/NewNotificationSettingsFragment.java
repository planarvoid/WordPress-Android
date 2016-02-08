package com.soundcloud.android.settings;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class NewNotificationSettingsFragment extends PreferenceFragment {

    public static NewNotificationSettingsFragment create() {
        return new NewNotificationSettingsFragment();
    }

    public NewNotificationSettingsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_new_notifications);
    }
}
