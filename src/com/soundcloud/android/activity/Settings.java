package com.soundcloud.android.activity;

import static android.provider.Settings.ACTION_WIRELESS_SETTINGS;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.cache.FileCache;
import com.soundcloud.android.service.SyncAdapterService;
import com.soundcloud.android.service.beta.BetaPreferences;
import com.soundcloud.android.utils.ChangeLog;
import com.soundcloud.android.utils.CloudUtils;

import android.app.ActivityManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

public class Settings extends PreferenceActivity {
    private static final int DIALOG_CACHE_DELETING      = 0;
    private static final int DIALOG_USER_DELETE_CONFIRM = 1;

    private ProgressDialog mDeleteDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        if (SoundCloudApplication.BETA_MODE) {
            BetaPreferences.add(this, getPreferenceScreen());
        }

        setClearCacheTitle();

        final ChangeLog cl = new ChangeLog(this);
        findPreference("changeLog").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        cl.getDialog(true).show();
                        return true;
                    }
                });

        findPreference("revokeAccess").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        safeShowDialog(DIALOG_USER_DELETE_CONFIRM);
                        return true;
                    }
                });
        findPreference("clearCache").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        new FileCache.DeleteCacheTask() {
                            @Override protected void onPreExecute() {
                                safeShowDialog(DIALOG_CACHE_DELETING);
                            }

                            @Override protected void onProgressUpdate(Integer... progress) {
                                if (mDeleteDialog != null) {
                                    mDeleteDialog.setIndeterminate(false);
                                    mDeleteDialog.setProgress(progress[0]);
                                    mDeleteDialog.setMax(progress[1]);
                                }
                            }

                            @Override protected void onPostExecute(Boolean result) {
                                removeDialog(DIALOG_CACHE_DELETING);
                                setClearCacheTitle();
                            }
                        }.execute(FileCache.getCacheDir(Settings.this));
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

        ListPreference recordingQuality = (ListPreference) findPreference("defaultRecordingQuality");
        recordingQuality.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        preference.setTitle(getString(R.string.pref_record_quality) + " (" + o + ")");
                        return true;
                    }
                }
        );

        recordingQuality.setTitle(getString(R.string.pref_record_quality) +
                " (" + recordingQuality.getValue() + ")");

        if (!SoundCloudApplication.DEV_MODE) {
            getPreferenceScreen().removePreference(findPreference("dev-settings"));
        } else {
            findPreference("dev.clearNotifications").setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            SyncAdapterService.requestNewSync(getApp());
                            return true;
                        }
                    });

            findPreference("dev.crash").setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            if (!ActivityManager.isUserAMonkey()) {
                                throw new RuntimeException("developer requested crash");
                            } else {
                                return true;
                            }
                        }
                    });
        }
    }

    public void safeShowDialog(int dialogId) {
        if (!isFinishing()) {
            showDialog(dialogId);
        }
    }

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    @Override
    protected void onResume() {
        getApp().trackPage(Consts.Tracking.SETTINGS);
        super.onResume();
    }

    private void setClearCacheTitle() {
        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                final String cacheSize = FileCache.cacheSizeInMbFormatted(Settings.this);
                handler.post(new Runnable() {
                    @Override public void run() {
                        findPreference("clearCache").setTitle(
                            getResources().getString(R.string.pref_clear_cache) +
                            " [" + cacheSize + " MB]");
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

            case DIALOG_USER_DELETE_CONFIRM:
                return CloudUtils.createLogoutDialog(this);
        }
        return super.onCreateDialog(id);
    }
}
