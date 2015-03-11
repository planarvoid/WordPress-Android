package com.soundcloud.android.settings;

import com.soundcloud.android.R;
import com.soundcloud.android.main.WebViewActivity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;

public class LegalActivity extends ScSettingsActivity {

    private final Preference.OnPreferenceClickListener clickListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final Intent intent = new Intent(LegalActivity.this, WebViewActivity.class);
            intent.setData(preference.getIntent().getData());
            startActivity(intent);
            return true;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.legal);

        findPreference("terms_of_service").setOnPreferenceClickListener(clickListener);
        findPreference("privacy_policy").setOnPreferenceClickListener(clickListener);
        findPreference("imprint").setOnPreferenceClickListener(clickListener);
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}