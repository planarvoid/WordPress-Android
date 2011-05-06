
package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.utils.CloudCache;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

public class Settings extends PreferenceActivity {
    private static final int DIALOG_CACHE_DELETED = 0;
    private static final int DIALOG_CACHE_DELETING = 1;
    private static final int DIALOG_USER_DELETE_CONFIRM = 2;

    private ProgressDialog mDeleteDialog;

    private DeleteCacheTask mDeleteTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.settings);
        setClearCacheTitle();

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
                        mDeleteTask = new DeleteCacheTask();
                        mDeleteTask.setActivity(Settings.this);
                        mDeleteTask.execute();
                        return true;
                    }
                });
        findPreference("wireless").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        try { // rare phones have no wifi settings
                            startActivity(new Intent(
                                    android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                        } catch (Exception e) {
                            Log.e(TAG, "error", e);
                        }
                        return true;
                    }
                });


        ListPreference recordingQuality = (ListPreference) findPreference("defaultRecordingQuality");

        recordingQuality.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        preference.setTitle(getString(R.string.pref_record_quality)+" ("+o+")");
                        return true;
                    }
                }
        );
        recordingQuality.setTitle(getString(R.string.pref_record_quality)+" ("+recordingQuality.getValue()+")");

        if (!SoundCloudApplication.DEV_MODE)
            this.getPreferenceScreen().removePreference(findPreference("defaultRecordingHighQualityType"));

    }

    public void safeShowDialog(int dialogId){
        if (!isFinishing()){
            showDialog(dialogId);
        }
    }

    @Override
    protected void onResume() {
        ((SoundCloudApplication)getApplication()).pageTrack("/settings");
        super.onResume();
    }

    private void setClearCacheTitle() {
        final Handler handler = new Handler();
        new Thread() {
            @Override public void run() {
                final String cacheSize = CloudCache.cacheSizeInMbString(Settings.this);
                handler.post(new Runnable() {
                    @Override public void run() {
                        Settings.this.findPreference("clearCache").setTitle(
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
            case DIALOG_CACHE_DELETED:
                return new AlertDialog.Builder(this).setTitle(R.string.cache_cleared)
                        .setPositiveButton(android.R.string.ok, null).create();
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

    public static class DeleteCacheTask extends CloudCache.DeleteCacheTask {
        @Override
        protected void onPreExecute() {
            Settings settings = (Settings) mActivityRef.get();
            if (settings != null) {
                settings.safeShowDialog(DIALOG_CACHE_DELETING);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
        Settings settings = (Settings) mActivityRef.get();
            if (settings != null) {
                settings.mDeleteDialog.setIndeterminate(false);
                settings.mDeleteDialog.setMax(progress[1]);
                settings.mDeleteDialog.setProgress(progress[0]);
            }

        }

        @Override
        protected void onPostExecute(Boolean result) {
            Settings settings = (Settings) mActivityRef.get();
            if (settings != null) {
                settings.removeDialog(DIALOG_CACHE_DELETING);
                settings.safeShowDialog(DIALOG_CACHE_DELETED);
                settings.setClearCacheTitle();
            }
        }
    }
}
