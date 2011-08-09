package com.soundcloud.android.activity;

import com.soundcloud.android.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class NotificationSettings extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.notifications_settings);
    }
}
