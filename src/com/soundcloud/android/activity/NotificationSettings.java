package com.soundcloud.android.activity;

import static android.provider.Settings.ACTION_WIRELESS_SETTINGS;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.cache.FileCache;
import com.soundcloud.utils.ChangeLog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

public class NotificationSettings extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.notifications_settings);
    }
}
