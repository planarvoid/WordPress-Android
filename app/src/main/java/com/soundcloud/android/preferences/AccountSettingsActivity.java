package com.soundcloud.android.preferences;

import com.soundcloud.android.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class AccountSettingsActivity extends PreferenceActivity {
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
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}
