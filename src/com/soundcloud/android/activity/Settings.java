package com.soundcloud.android.activity;

import static android.provider.Settings.ACTION_WIRELESS_SETTINGS;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.c2dm.C2DMReceiver;
import com.soundcloud.android.cache.FileCache;
import com.soundcloud.android.model.User;
import com.soundcloud.android.service.beta.BetaPreferences;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.sync.SyncAdapterService;
import com.soundcloud.android.utils.ChangeLog;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.IOUtils;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class Settings extends PreferenceActivity {
    private static final int DIALOG_CACHE_DELETING = 0;
    private static final int DIALOG_USER_LOGOUT_CONFIRM = 1;

    private ProgressDialog mDeleteDialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        if (SoundCloudApplication.BETA_MODE) {
            BetaPreferences.add(this, getPreferenceScreen());
        }

        findPreference("tour").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(Settings.this, Tour.class);
                        startActivity(intent);
                        return true;
                    }
                });


        final ChangeLog cl = new ChangeLog(this);
        findPreference("changeLog").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        cl.getDialog(true).show();
                        return true;
                    }
                });

        findPreference("logout").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        if (!CloudUtils.isUserAMonkey()) {
                            // don't let the monkey log out
                            safeShowDialog(DIALOG_USER_LOGOUT_CONFIRM);
                        }
                        return true;
                    }
                });

        findPreference("help").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://help.soundcloud.com/"));
                        startActivity(browserIntent);
                        return true;
                    }
                });

        findPreference("clearCache").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        new FileCache.DeleteCacheTask(false) {
                            @Override
                            protected void onPreExecute() {
                                safeShowDialog(DIALOG_CACHE_DELETING);
                            }

                            @Override
                            protected void onProgressUpdate(Integer... progress) {
                                if (mDeleteDialog != null) {
                                    mDeleteDialog.setIndeterminate(false);
                                    mDeleteDialog.setProgress(progress[0]);
                                    mDeleteDialog.setMax(progress[1]);
                                }
                            }

                            @Override
                            protected void onPostExecute(Boolean result) {
                                removeDialog(DIALOG_CACHE_DELETING);
                                updateClearCacheTitles();
                            }
                        }.execute(IOUtils.getCacheDir(Settings.this));
                        return true;
                    }
                });

        findPreference("clearStreamCache").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        new FileCache.DeleteCacheTask(true) {
                            @Override
                            protected void onPreExecute() {
                                safeShowDialog(DIALOG_CACHE_DELETING);
                            }

                            @Override
                            protected void onProgressUpdate(Integer... progress) {
                                if (mDeleteDialog != null) {
                                    mDeleteDialog.setIndeterminate(false);
                                    mDeleteDialog.setProgress(progress[0]);
                                    mDeleteDialog.setMax(progress[1]);
                                }
                            }

                            @Override
                            protected void onPostExecute(Boolean result) {
                                removeDialog(DIALOG_CACHE_DELETING);
                                updateClearCacheTitles();
                            }
                        }.execute(Consts.EXTERNAL_STREAM_DIRECTORY);
                        return true;
                    }
                });


        findPreference("wireless").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        try { // rare phones have no wifi settings
                            startActivity(new Intent(ACTION_WIRELESS_SETTINGS));
                        } catch (ActivityNotFoundException e) {
                            Log.e(TAG, "error", e);
                        }
                        return true;
                    }
                });


        findPreference("about").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        startActivity(new Intent(Settings.this, About.class));
                        return true;
                    }
                });

        final ListPreference recordingQuality = (ListPreference) findPreference("defaultRecordingQuality");
        recordingQuality.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        CharSequence label = recordingQuality.getEntries()[recordingQuality.findIndexOfValue(o.toString())];
                        preference.setTitle(getString(R.string.pref_record_quality) + " (" + label + ")");
                        return true;
                    }
                }
        );

        recordingQuality.setTitle(getString(R.string.pref_record_quality) +
                " (" + recordingQuality.getEntry() + ")");

        if (!SoundCloudApplication.DEV_MODE) {
            getPreferenceScreen().removePreference(findPreference("dev-settings"));
        } else {
            findPreference("dev.clearNotifications").setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            SyncAdapterService.requestNewSync(getApp(), SyncAdapterService.CLEAR_ALL);
                            return true;
                        }
                    });

            findPreference("dev.rewindNotifications").setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            SyncAdapterService.requestNewSync(getApp(), SyncAdapterService.REWIND_LAST_DAY);
                            return true;
                        }
                    });


            findPreference("dev.syncNow").setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            SyncAdapterService.requestNewSync(getApp(), -1);
                            return true;
                        }
                    });


            findPreference("dev.crash").setOnPreferenceClickListener(
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

            findPreference("dev.http.proxy").setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override public boolean onPreferenceChange(Preference preference, Object s) {
                            if (!TextUtils.isEmpty(s.toString())) {
                                try {
                                    URL proxy = new URL(s.toString());
                                    if (!"https".equals(proxy.getProtocol()) &&
                                        !"http".equals(proxy.getProtocol()))  {
                                        throw new MalformedURLException("Need http/https url");
                                    }
                                } catch (MalformedURLException e) {
                                    Toast.makeText(Settings.this, R.string.pref_dev_http_proxy_invalid_url, Toast.LENGTH_SHORT).show();
                                    return false;
                                }
                            }
                            final Intent intent = new Intent(AndroidCloudAPI.Wrapper.CHANGE_PROXY_ACTION);
                            if (!TextUtils.isEmpty(s.toString())) intent.putExtra("proxy", s.toString());
                            sendBroadcast(intent);
                            return true;
                        }
                    }
            );
        }
    }

    public void safeShowDialog(int dialogId) {
        if (!isFinishing()) {
            showDialog(dialogId);
        }
    }

    private void updateClearCacheTitles() {
        setClearCacheTitle("clearCache", R.string.pref_clear_cache, IOUtils.getCacheDir(this));
        setClearCacheTitle("clearStreamCache", R.string.pref_clear_stream_cache, Consts.EXTERNAL_STREAM_DIRECTORY);
    }

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    @Override
    protected void onResume() {
        getApp().trackPage(Consts.Tracking.SETTINGS);
        updateClearCacheTitles();
        super.onResume();
    }

    private void setClearCacheTitle(final String pref, final int key, final File dir) {
        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                final String size = IOUtils.inMbFormatted(dir);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        findPreference(pref).setTitle(
                                getResources().getString(key) +
                                        " [" + size + " MB]");
                    }
                });
            }
        }.start();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_CACHE_DELETING:
                if (mDeleteDialog == null) {
                    mDeleteDialog = new ProgressDialog(this);
                    mDeleteDialog.setTitle(R.string.cache_clearing);
                    mDeleteDialog.setMessage(getString(R.string.cache_clearing_message));
                    mDeleteDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    mDeleteDialog.setIndeterminate(true);
                    mDeleteDialog.setCancelable(false);
                }
                return mDeleteDialog;

            case DIALOG_USER_LOGOUT_CONFIRM:
                return createLogoutDialog(this);
        }
        return super.onCreateDialog(id);
    }

    /* package */ static AlertDialog createLogoutDialog(final Activity a) {
        return new AlertDialog.Builder(a).setTitle(R.string.menu_clear_user_title)
                .setMessage(R.string.menu_clear_user_desc).setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                SoundCloudApplication app = (SoundCloudApplication) a.getApplication();
                                a.sendBroadcast(new Intent(CloudPlaybackService.RESET_ALL));
                                User.clearLoggedInUserFromStorage(app);
                                app.trackPage(Consts.Tracking.LOGGED_OUT);
                                app.trackEvent(Consts.Tracking.Categories.AUTH, "logout");

                                C2DMReceiver.unregister(a);

                                app.clearSoundCloudAccount(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                a.finish();
                                            }
                                        },
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                new AlertDialog.Builder(a)
                                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                                        .setMessage(R.string.settings_error_revoking_account_message)
                                                        .setPositiveButton(android.R.string.ok, null)
                                                        .create()
                                                        .show();
                                            }
                                        }
                                );
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }
}
