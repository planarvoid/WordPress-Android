package com.soundcloud.android.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.soundcloud.android.R;

public class AccountPreferences extends PreferenceActivity{
     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);

         addPreferencesFromResource(R.xml.account_settings);
     }
}
