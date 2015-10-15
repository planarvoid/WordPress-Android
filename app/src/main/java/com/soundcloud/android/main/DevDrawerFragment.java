package com.soundcloud.android.main;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.policies.DailyUpdateService;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.sync.SyncAdapterService;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.rx.eventbus.EventBus;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class DevDrawerFragment extends PreferenceFragment {

    private static final int SYNC_CLEAR_MODE_NOT_SET = -1;

    @Inject EventBus eventBus;
    @Inject FeatureFlags featureFlags;

    public DevDrawerFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.dev_drawer_prefs);
        addActions();
        addFeatureToggles();
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

        screen.findPreference(getString(R.string.dev_drawer_action_kill_app_key))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        android.os.Process.killProcess(android.os.Process.myPid());
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

        screen.findPreference(getString(R.string.dev_drawer_action_policy_sync_key))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        DailyUpdateService.start(getActivity().getApplicationContext());
                        return true;
                    }
                });

        screen.findPreference(getString(R.string.dev_drawer_action_rewind_notifications_key))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        SyncAdapterService.requestNewSync(getApp(), SyncAdapterService.REWIND_LAST_DAY);
                        return true;
                    }
                });

        screen.findPreference(getString(R.string.dev_drawer_action_clear_notifications_key))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        SyncAdapterService.requestNewSync(getApp(), SyncAdapterService.CLEAR_ALL);
                        return true;
                    }
                });

        screen.findPreference(getString(R.string.dev_drawer_action_sync_now_key))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        SyncAdapterService.requestNewSync(getApp(), SYNC_CLEAR_MODE_NOT_SET);
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

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getActivity().getApplication();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        view.setBackgroundColor(getResources().getColor(android.R.color.white));
        return view;
    }
}
