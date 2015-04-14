package com.soundcloud.android.settings;

import static android.preference.Preference.OnPreferenceClickListener;
import static com.soundcloud.android.settings.SettingKey.DEV_CLEAR_NOTIFICATIONS;
import static com.soundcloud.android.settings.SettingKey.DEV_CONFIG_FEATURES;
import static com.soundcloud.android.settings.SettingKey.DEV_CRASH;
import static com.soundcloud.android.settings.SettingKey.DEV_HTTP_PROXY;
import static com.soundcloud.android.settings.SettingKey.DEV_RECORDING_TYPE;
import static com.soundcloud.android.settings.SettingKey.DEV_REWIND_NOTIFICATIONS;
import static com.soundcloud.android.settings.SettingKey.DEV_SYNC_NOW;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.sync.SyncAdapterService;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.SharedPreferencesUtils;

import android.app.Activity;
import android.content.Intent;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.widget.Toast;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;

class DeveloperSettings implements OnPreferenceClickListener {

    private final SoundCloudApplication application;

    private PreferenceFragment settings;

    @Inject
    public DeveloperSettings(SoundCloudApplication application) {
        this.application = application;
    }

    public void addTo(final PreferenceFragment settings) {
        this.settings = settings;
        settings.addPreferencesFromResource(R.xml.settings_dev);
        setupPreferenceListeners(settings);
    }

    private void setupPreferenceListeners(final PreferenceFragment settings) {

        settings.findPreference(DEV_CLEAR_NOTIFICATIONS).setOnPreferenceClickListener(this);
        settings.findPreference(DEV_REWIND_NOTIFICATIONS).setOnPreferenceClickListener(this);
        settings.findPreference(DEV_SYNC_NOW).setOnPreferenceClickListener(this);
        settings.findPreference(DEV_CRASH).setOnPreferenceClickListener(this);
        settings.findPreference(DEV_CONFIG_FEATURES).setOnPreferenceClickListener(this);

        settings.findPreference(DEV_HTTP_PROXY).setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (!TextUtils.isEmpty(newValue.toString())) {
                            changeProxy(newValue.toString());
                        }
                        return true;
                    }
                }
        );

        SharedPreferencesUtils.listWithLabel((ListPreference) settings.findPreference(DEV_RECORDING_TYPE),
                R.string.pref_dev_record_type);
    }

    private boolean changeProxy(String address) {
        try {
            URL proxy = new URL(address);
            if (!"https".equals(proxy.getProtocol()) && !"http".equals(proxy.getProtocol())) {
                throw new MalformedURLException("Need http/https url");
            }
        } catch (MalformedURLException e) {
            Toast.makeText(application, R.string.pref_dev_http_proxy_invalid_url, Toast.LENGTH_SHORT).show();
            return true;
        }
        final Intent intent = new Intent(Actions.CHANGE_PROXY_ACTION);
        intent.putExtra("proxy", address);
        settings.getActivity().sendBroadcast(intent);
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        final Activity parent = settings.getActivity();
        switch (preference.getKey()) {
            case DEV_CLEAR_NOTIFICATIONS:
                SyncAdapterService.requestNewSync(application, SyncAdapterService.CLEAR_ALL);
                return true;
            case DEV_REWIND_NOTIFICATIONS:
                SyncAdapterService.requestNewSync(application, SyncAdapterService.REWIND_LAST_DAY);
                return true;
            case DEV_SYNC_NOW:
                SyncAdapterService.requestNewSync(application, -1);
                return true;
            case DEV_CRASH:
                if (!AndroidUtils.isUserAMonkey()) {
                    throw new RuntimeException("Developer requested crash");
                }
                return true;
            case DEV_CONFIG_FEATURES:
                parent.startActivity(new Intent(parent, ConfigurationFeaturesActivity.class));
                return true;
            default:
                return false;
        }
    }

}
