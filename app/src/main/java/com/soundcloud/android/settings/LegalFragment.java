package com.soundcloud.android.settings;

import static android.preference.Preference.OnPreferenceClickListener;
import static com.soundcloud.android.settings.SettingKey.GO_TERMS;
import static com.soundcloud.android.settings.SettingKey.IMPRINT;
import static com.soundcloud.android.settings.SettingKey.PRIVACY_POLICY;
import static com.soundcloud.android.settings.SettingKey.TERMS_OF_SERVICE;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.main.WebViewActivity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import javax.inject.Inject;

public class LegalFragment extends PreferenceFragment implements OnPreferenceClickListener {

    @Inject FeatureOperations featureOperations;

    public static LegalFragment create() {
        return new LegalFragment();
    }

    public LegalFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.legal);

        findPreference(TERMS_OF_SERVICE).setOnPreferenceClickListener(this);
        findPreference(PRIVACY_POLICY).setOnPreferenceClickListener(this);
        findPreference(IMPRINT).setOnPreferenceClickListener(this);

        hideGoTermsForFreeUser();
    }

    private void hideGoTermsForFreeUser() {
        if (featureOperations.getCurrentPlan().equals(Plan.FREE_TIER)) {
            getPreferenceScreen().removePreference(findPreference(GO_TERMS));
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        final Intent intent = new Intent(getActivity(), WebViewActivity.class);
        intent.setData(preference.getIntent().getData());
        startActivity(intent);
        return true;
    }

}
