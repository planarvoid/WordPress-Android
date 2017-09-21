package com.soundcloud.android.more;

import static com.soundcloud.android.settings.SettingKey.CLEAR_CACHE;
import static com.soundcloud.android.settings.SettingKey.CLEAR_SEARCH_HISTORY;
import static com.soundcloud.android.settings.SettingKey.SYNC_WIFI_ONLY;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.search.history.SearchHistoryStorage;
import com.soundcloud.android.settings.ClearCacheDialog;
import com.soundcloud.android.utils.LeakCanaryWrapper;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import javax.inject.Inject;

public class BasicSettingsFragment extends PreferenceFragment {

    @Inject FeatureFlags featureFlags;
    @Inject LeakCanaryWrapper leakCanaryWrapper;
    @Inject ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;
    @Inject SearchHistoryStorage searchHistoryStorage;

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
        setupSyncWifiOnlyPreference();
        setupClearSearchHistoryPreference();
    }

    private void setupClearSearchHistoryPreference() {
        Preference preference = getPreferenceScreen().findPreference(CLEAR_SEARCH_HISTORY);
        if (featureFlags.isEnabled(Flag.LOCAL_SEARCH_HISTORY)) {
            preference.setEnabled(true);
            preference.setOnPreferenceClickListener(ignored -> {
                searchHistoryStorage.clear();
                Toast.makeText(getActivity(), R.string.search_history_cleared, Toast.LENGTH_SHORT).show();
                return true;
            });
        } else {
            getPreferenceScreen().removePreference(preference);
        }
    }

    private void setupClearCachePreference() {
        getPreferenceScreen().findPreference(CLEAR_CACHE)
                             .setOnPreferenceClickListener(preference -> {
                                 ClearCacheDialog.show(getFragmentManager());
                                 return true;
                             });
    }

    private void setupSyncWifiOnlyPreference() {
        getPreferenceScreen().findPreference(SYNC_WIFI_ONLY)
                             .setSummary(changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.PREF_SYNC_WIFI_ONLY_DESCRIPTION));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        leakCanaryWrapper.watch(this);
    }
}
