package com.soundcloud.android.main;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.ConfigurationManager;
import com.soundcloud.android.policies.DailyUpdateService;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.rx.eventbus.EventBus;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class DevDrawerFragment extends PreferenceFragment {

    private static final String DEVICE_CONFIG_SETTINGS = "device_config_settings";
    private static final String KEY_LAST_CONFIG_CHECK_TIME = "last_config_check_time";

    @Inject EventBus eventBus;
    @Inject FeatureFlags featureFlags;
    @Inject AccountOperations accountOperations;
    @Inject DevDrawerExperimentsHelper drawerExperimentsHelper;
    @Inject ConfigurationManager configurationManager;
    @Inject Navigator navigator;
    private SharedPreferences.OnSharedPreferenceChangeListener configurationUpdateListener;

    public DevDrawerFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.dev_drawer_prefs);
        addActions();
        addFeatureToggles();
        drawerExperimentsHelper.addExperiments(getPreferenceScreen());
    }

    private void addFeatureToggles() {
        PreferenceScreen screen = this.getPreferenceScreen();
        PreferenceCategory category = new PreferenceCategory(screen.getContext());
        category.setTitle(getString(R.string.dev_drawer_section_build_features));
        screen.addPreference(category);

        for (Flag flag : Flag.realFeatures()) {
            CheckBoxPreference checkBoxPref = new CheckBoxPreference(screen.getContext());
            checkBoxPref.setKey(featureFlags.getPreferenceKey(flag));
            checkBoxPref.setTitle(ScTextUtils.fromSnakeCaseToCamelCase(flag.name()));
            checkBoxPref.setChecked(featureFlags.isEnabled(flag));
            category.addPreference(checkBoxPref);
        }
    }

    private void addActions() {
        final PreferenceScreen screen = this.getPreferenceScreen();

        screen.findPreference(getString(R.string.dev_drawer_action_get_oauth_token_to_clipboard_key))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        copyTokenToClipboard();
                        Toast.makeText(getActivity(), R.string.dev_oauth_token_copied, Toast.LENGTH_LONG).show();
                        return true;
                    }
                });

        screen.findPreference(getString(R.string.dev_drawer_action_kill_app_key))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        navigator.restartApp(getActivity());
                        return true;
                    }
                });

        screen.findPreference(getString(R.string.dev_drawer_action_upgrade_flow_key))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        navigator.restartForAccountUpgrade(getActivity());
                        return true;
                    }
                });

        screen.findPreference(getString(R.string.dev_drawer_action_reset_flags_key))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        for (Flag flag : Flag.realFeatures()) {
                            final String preferenceKey = featureFlags.getPreferenceKey(flag);
                            final CheckBoxPreference chkPreference = (CheckBoxPreference) screen.findPreference(preferenceKey);
                            chkPreference.setChecked(featureFlags.resetAndGet(flag));
                        }
                        return true;
                    }
                });

        setupForceConfigUpdatePref(screen);

        screen.findPreference(getString(R.string.dev_drawer_action_policy_sync_key))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        DailyUpdateService.start(getActivity().getApplicationContext());
                        return true;
                    }
                });


        screen.findPreference(getString(R.string.dev_drawer_action_crash_key))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if (!AndroidUtils.isUserAMonkey()) {
                            throw new RuntimeException("Developer requested crash");
                        }
                        return true;
                    }
                });

    }

    private void setupForceConfigUpdatePref(PreferenceScreen screen) {
        final Preference updateConfigPref = screen.findPreference(getString(R.string.dev_drawer_action_config_update_key));
        updateConfigPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                configurationManager.forceUpdate();
                return true;
            }
        });
        final SharedPreferences sharedPrefs = getActivity().getSharedPreferences(DEVICE_CONFIG_SETTINGS, Context.MODE_PRIVATE);
        configurationUpdateListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (KEY_LAST_CONFIG_CHECK_TIME.equals(key)) {
                    updateLastConfigUpdateText(updateConfigPref, sharedPreferences);
                }
            }
        };
        sharedPrefs.registerOnSharedPreferenceChangeListener(configurationUpdateListener);
        updateLastConfigUpdateText(updateConfigPref, sharedPrefs);
    }

    private void updateLastConfigUpdateText(Preference preference, SharedPreferences sharedPreferences) {
        final long lastUpdatedTs = sharedPreferences.getLong(KEY_LAST_CONFIG_CHECK_TIME, 0);
        preference.setSummary("last updated " + ScTextUtils.formatTimeElapsedSince(getResources(), lastUpdatedTs, true));
    }

    private void copyTokenToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("oauth_token", accountOperations.getSoundCloudToken().getAccessToken());
        clipboard.setPrimaryClip(clip);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        view.setBackgroundColor(getResources().getColor(android.R.color.white));
        return view;
    }
}
