package com.soundcloud.android.settings.notifications;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class NotificationPreferencesFragment extends PreferenceFragment {

    public static NotificationPreferencesFragment create() {
        return new NotificationPreferencesFragment();
    }

    public NotificationPreferencesFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.notification_preferences);
    }
}
