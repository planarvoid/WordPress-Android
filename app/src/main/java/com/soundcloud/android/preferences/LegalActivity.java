package com.soundcloud.android.preferences;

import com.soundcloud.android.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class LegalActivity extends PreferenceActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.legal);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}