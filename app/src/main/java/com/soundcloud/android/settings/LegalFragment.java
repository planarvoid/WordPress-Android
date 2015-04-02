package com.soundcloud.android.settings;

import static android.preference.Preference.OnPreferenceClickListener;
import static com.soundcloud.android.settings.SettingKey.IMPRINT;
import static com.soundcloud.android.settings.SettingKey.PRIVACY_POLICY;
import static com.soundcloud.android.settings.SettingKey.TERMS_OF_SERVICE;

import com.soundcloud.android.R;
import com.soundcloud.android.main.WebViewActivity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

public class LegalFragment extends PreferenceFragment implements OnPreferenceClickListener {

    public static LegalFragment create() {
        return new LegalFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.legal);

        findPreference(TERMS_OF_SERVICE).setOnPreferenceClickListener(this);
        findPreference(PRIVACY_POLICY).setOnPreferenceClickListener(this);
        findPreference(IMPRINT).setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        final Intent intent = new Intent(getActivity(), WebViewActivity.class);
        intent.setData(preference.getIntent().getData());
        startActivity(intent);
        return true;
    }

}
