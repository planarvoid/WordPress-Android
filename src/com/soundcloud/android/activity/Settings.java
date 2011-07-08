package com.soundcloud.android.activity;

import static android.provider.Settings.ACTION_WIRELESS_SETTINGS;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.cache.FileCache;
import com.soundcloud.android.service.beta.BetaService;
import com.soundcloud.utils.ChangeLog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Settings extends PreferenceActivity {
    private static final int DIALOG_CACHE_DELETING      = 0;
    private static final int DIALOG_USER_DELETE_CONFIRM = 1;

    private ProgressDialog mDeleteDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        if (SoundCloudApplication.BETA_MODE) {
            Preference beta = new Preference(this);
            beta.setTitle(R.string.pref_beta);
            beta.setSummary(R.string.pref_beta_summary);

            CheckBoxPreference autoUpdate = new CheckBoxPreference(this);
            autoUpdate.setTitle(R.string.pref_beta_check_for_updates);
            autoUpdate.setSummary(R.string.pref_beta_check_for_updates_summary);
            autoUpdate.setKey(BetaService.PREF_CHECK_UPDATES);

            getPreferenceScreen().addPreference(beta);
            getPreferenceScreen().addPreference(autoUpdate);
            /*
            // does not work - android styling bug

            PreferenceScreen screen = preferenceScreenFromResource(R.xml.settings_beta);
            if (screen != null) {
                getPreferenceScreen().addPreference(screen);
            }
            */
        }

        setClearCacheTitle();

        final ChangeLog cl = new ChangeLog(this);
        if (cl.firstRun()) cl.getLogDialog().show();

        findPreference("changeLog").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        cl.getFullLogDialog().show();
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
                                mDeleteDialog.setIndeterminate(false);
                                mDeleteDialog.setProgress(progress[0]);
                                mDeleteDialog.setMax(progress[1]);
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
                        } catch (Exception e) {
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
        recordingQuality.setTitle(getString(R.string.pref_record_quality) + " (" + recordingQuality.getValue() + ")");

        if (!SoundCloudApplication.DEV_MODE) {
            this.getPreferenceScreen().removePreference(findPreference("defaultRecordingHighQualityType"));
            this.getPreferenceScreen().removePreference(findPreference("dashboardMaxStored"));
        }
    }

    // hacky way to use package private method on PreferenceManager
    private PreferenceScreen preferenceScreenFromResource(int settings_beta) {
        try {
            Class pm = getPreferenceManager().getClass();
            Method m = pm.getDeclaredMethod("inflateFromResource", Context.class, Integer.TYPE, PreferenceScreen.class);
            m.setAccessible(true);
            return (PreferenceScreen) m.invoke(getPreferenceManager(), this, settings_beta, null);
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "error", e);
            return null;
        } catch (IllegalAccessException e) {
            Log.w(TAG, "error", e);
            return null;
        } catch (InvocationTargetException e) {
            Log.w(TAG, "error", e);
            return null;
        }
    }

    public void safeShowDialog(int dialogId) {
        if (!isFinishing()) {
            showDialog(dialogId);
        }
    }

    @Override
    protected void onResume() {
        ((SoundCloudApplication) getApplication()).pageTrack("/settings");
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
                return new AlertDialog.Builder(this).setTitle(R.string.menu_clear_user_title)
                        .setMessage(R.string.menu_clear_user_desc).setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        ((SoundCloudApplication) getApplication()).clearSoundCloudAccount(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        finish();
                                                    }
                                                },
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        new AlertDialog.Builder(Settings.this)
                                                                .setIcon(android.R.drawable.ic_dialog_alert)
                                                                .setMessage(R.string.settings_error_revoking_account_message)
                                                                .setTitle(R.string.settings_error_revoking_account_title)
                                                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialog, int which) {
                                                                        // finish();
                                                                    }
                                                                }).create().show();
                                                    }
                                                }
                                        );
                                    }
                                }).setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                    }
                                }).create();
        }
        return super.onCreateDialog(id);
    }
}
