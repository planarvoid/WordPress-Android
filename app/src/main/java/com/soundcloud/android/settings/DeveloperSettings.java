package com.soundcloud.android.settings;

import static android.preference.Preference.OnPreferenceClickListener;
import static com.soundcloud.android.settings.SettingKey.DEV_CLEAR_NOTIFICATIONS;
import static com.soundcloud.android.settings.SettingKey.DEV_CLEAR_RECORDINGS;
import static com.soundcloud.android.settings.SettingKey.DEV_CONFIG_FEATURES;
import static com.soundcloud.android.settings.SettingKey.DEV_CRASH;
import static com.soundcloud.android.settings.SettingKey.DEV_HTTP_PROXY;
import static com.soundcloud.android.settings.SettingKey.DEV_RECORDING_TYPE;
import static com.soundcloud.android.settings.SettingKey.DEV_REWIND_NOTIFICATIONS;
import static com.soundcloud.android.settings.SettingKey.DEV_SYNC_NOW;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncAdapterService;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.SharedPreferencesUtils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
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
        settings.findPreference(DEV_CLEAR_RECORDINGS).setOnPreferenceClickListener(this);
        settings.findPreference(DEV_CLEAR_RECORDINGS).setOnPreferenceClickListener(this);

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
            case DEV_CLEAR_RECORDINGS:
                showClearRecordingsDialog(parent);
                return true;
            default:
                return false;
        }
    }

    private void showClearRecordingsDialog(final Activity parent) {
        new AlertDialog.Builder(parent)
                .setMessage(R.string.dev_clear_recordings)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new DeleteRecordings(application)).start();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create()
                .show();
    }

    private static class DeleteRecordings implements Runnable {
        private final Context context;
        private final Handler handler = new Handler();

        private DeleteRecordings(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            synchronized (DeleteRecordings.class) {
                IOUtils.deleteDir(SoundRecorder.RECORD_DIR);
                IOUtils.mkdirs(SoundRecorder.RECORD_DIR);
                final int count = context.getContentResolver().delete(Content.RECORDINGS.uri, null, null);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        AndroidUtils.showToast(context, "Deleted " + count + " recordings");
                    }
                });
            }
        }
    }

}
