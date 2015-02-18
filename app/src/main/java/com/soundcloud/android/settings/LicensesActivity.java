package com.soundcloud.android.settings;

import com.soundcloud.android.R;

import android.os.Bundle;

public class LicensesActivity extends ScSettingsActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.licenses);
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}