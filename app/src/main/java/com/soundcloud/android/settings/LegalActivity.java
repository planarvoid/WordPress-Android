package com.soundcloud.android.settings;

import com.soundcloud.android.R;

import android.os.Bundle;

public class LegalActivity extends ScSettingsActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.legal);
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}