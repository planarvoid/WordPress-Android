package com.soundcloud.android.activity;

import static android.provider.Settings.ACTION_WIRELESS_SETTINGS;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.c2dm.C2DMReceiver;
import com.soundcloud.android.cache.FileCache;
import com.soundcloud.android.model.User;
import com.soundcloud.android.service.beta.BetaPreferences;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.utils.ChangeLog;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.SharedPreferencesUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

import java.io.File;

@Tracking(page = Page.Settings_main)
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

        findPreference("accountSyncSettings").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        getApp().track(Page.Settings_account_sync);
                        return false;
                    }
        });

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
                        getApp().track(Page.Settings_change_log);
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

        SharedPreferencesUtils.listWithLabel(this,
                R.string.pref_record_quality,
                "defaultRecordingQuality");

        if (!SoundCloudApplication.DEV_MODE) {
            getPreferenceScreen().removePreference(findPreference(DevSettings.PREF_KEY));
        } else {
            DevSettings.setup(this, getApp());
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
        getApp().track(getClass());
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
        final SoundCloudApplication app = (SoundCloudApplication) a.getApplication();
        app.track(Click.Log_out_log_out);
        return new AlertDialog.Builder(a).setTitle(R.string.menu_clear_user_title)
                .setMessage(R.string.menu_clear_user_desc)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                a.sendBroadcast(new Intent(Actions.LOGGING_OUT));
                                a.sendBroadcast(new Intent(CloudPlaybackService.RESET_ALL));
                                app.track(Click.Log_out_box_ok);
                                User.clearLoggedInUserFromStorage(app);
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
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        app.track(Click.Log_out_box_cancel);
                    }
                })
                .create();
    }
}
