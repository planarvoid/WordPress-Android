package com.soundcloud.android.preferences;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.sync.SyncAdapterService;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.SharedPreferencesUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

public final class DevSettings {
    public static final String PREF_KEY = "dev-settings";

    public static final String DEV_ENABLE_SKIPPY        = "dev.enableSkippy";
    public static final String DEV_CLEAR_NOTIFICATIONS  = "dev.clearNotifications";
    public static final String DEV_REWIND_NOTIFICATIONS = "dev.rewindNotifications";
    public static final String DEV_SYNC_NOW             = "dev.syncNow";
    public static final String DEV_CRASH                = "dev.crash";
    public static final String DEV_CLEAR_RECORDINGS     = "dev.clearRecordings";
    public static final String DEV_HTTP_PROXY           = "dev.http.proxy";
    public static final String DEV_RECORDING_TYPE       = "dev.defaultRecordingType";
    public static final String DEV_RECORDING_TYPE_RAW   = "raw";

    private DevSettings() {
    }

    public static void setup(final PreferenceActivity activity, final SoundCloudApplication app) {
        activity.findPreference(DEV_CLEAR_NOTIFICATIONS).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        SyncAdapterService.requestNewSync(app, SyncAdapterService.CLEAR_ALL);
                        return true;
                    }
                });

        activity.findPreference(DEV_REWIND_NOTIFICATIONS).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        SyncAdapterService.requestNewSync(app, SyncAdapterService.REWIND_LAST_DAY);
                        return true;
                    }
                });


        activity.findPreference(DEV_SYNC_NOW).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        SyncAdapterService.requestNewSync(app, -1);
                        return true;
                    }
                });


        activity.findPreference(DEV_CRASH).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if (!AndroidUtils.isUserAMonkey()) {
                            throw new RuntimeException("developer requested crash");
                        } else {
                            return true;
                        }
                    }
                });

        activity.findPreference(DEV_HTTP_PROXY).setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object s) {
                        if (!TextUtils.isEmpty(s.toString())) {
                            try {
                                URL proxy = new URL(s.toString());
                                if (!"https".equals(proxy.getProtocol()) &&
                                        !"http".equals(proxy.getProtocol())) {
                                    throw new MalformedURLException("Need http/https url");
                                }
                            } catch (MalformedURLException e) {
                                Toast.makeText(activity, R.string.pref_dev_http_proxy_invalid_url, Toast.LENGTH_SHORT).show();
                                return false;
                            }
                        }
                        final Intent intent = new Intent(Actions.CHANGE_PROXY_ACTION);
                        if (!TextUtils.isEmpty(s.toString())) intent.putExtra("proxy", s.toString());
                        activity.sendBroadcast(intent);
                        return true;
                    }
                }
        );

        activity.findPreference(DEV_CLEAR_RECORDINGS).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(activity).setMessage(R.string.dev_clear_recordings)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new Thread(new DeleteRecordings(activity.getApplicationContext())).start();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create()
                        .show();
                return false;
            }
        });

        SharedPreferencesUtils.listWithLabel((ListPreference) activity.findPreference(DEV_RECORDING_TYPE),
                R.string.pref_dev_record_type);
    }

    private static class DeleteRecordings implements Runnable {
        private final Context mContext;
        private Handler handler = new Handler();

        private DeleteRecordings(Context context) {
            this.mContext = context;
        }

        @Override
        public void run() {
            synchronized (DeleteRecordings.class) {
                IOUtils.deleteDir(SoundRecorder.RECORD_DIR);
                IOUtils.mkdirs(SoundRecorder.RECORD_DIR);
                final int count = mContext.getContentResolver().delete(Content.RECORDINGS.uri, null, null);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        AndroidUtils.showToast(mContext, "Deleted " + count + " recordings");
                    }
                });
            }
        }
    }
}
