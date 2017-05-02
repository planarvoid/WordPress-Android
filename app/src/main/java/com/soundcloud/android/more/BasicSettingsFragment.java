package com.soundcloud.android.more;

import static com.soundcloud.android.settings.SettingKey.CLEAR_CACHE;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.settings.ClearCacheDialog;
import com.soundcloud.android.utils.LeakCanaryWrapper;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import javax.inject.Inject;

public class BasicSettingsFragment extends PreferenceFragment {

    @Inject FeatureFlags featureFlags;
    @Inject LeakCanaryWrapper leakCanaryWrapper;

    public static BasicSettingsFragment create() {
        return new BasicSettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SoundCloudApplication.getObjectGraph().inject(this);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings_basic);
        setupClearCachePreference();
    }

    private void setupClearCachePreference() {
        getPreferenceScreen().findPreference(CLEAR_CACHE)
                             .setOnPreferenceClickListener(preference -> {
                                 ClearCacheDialog.show(getFragmentManager());
                                 return true;
                             });
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        leakCanaryWrapper.watch(this);
    }
}
