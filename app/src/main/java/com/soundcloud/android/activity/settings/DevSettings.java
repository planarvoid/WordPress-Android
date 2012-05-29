package com.soundcloud.android.activity.settings;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.service.sync.SyncAdapterService;
import com.soundcloud.android.utils.CloudUtils;

import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

public final class DevSettings {
    public static final String PREF_KEY = "dev-settings";

    public static final String DEV_CLEAR_NOTIFICATIONS  = "dev.clearNotifications";
    public static final String DEV_REWIND_NOTIFICATIONS = "dev.rewindNotifications";
    public static final String DEV_SYNC_NOW             = "dev.syncNow";
    public static final String DEV_CRASH                = "dev.crash";
    public static final String DEV_HTTP_PROXY           = "dev.http.proxy";
    public static final String DEV_RECORDING_TYPE       = "dev.defaultRecordingHighQualityType";

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
                        if (!CloudUtils.isUserAMonkey()) {
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
    }
}
