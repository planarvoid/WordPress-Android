package com.soundcloud.android.activity.settings;

import static android.provider.Settings.ACTION_WIRELESS_SETTINGS;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.activity.ActionBarController;
import com.soundcloud.android.cache.FileCache;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.ScObserver;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ChangeLog;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.SharedPreferencesUtils;
import org.jetbrains.annotations.NotNull;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;

import java.io.File;

@Tracking(page = Page.Settings_main)
public class Settings extends SherlockPreferenceActivity implements ActionBarController.ActionBarOwner {
    private static final int DIALOG_CACHE_DELETING = 0;
    private static final int DIALOG_USER_LOGOUT_CONFIRM = 1;

    public static final String CHANGE_LOG = "changeLog";
    public static final String LOGOUT = "logout";
    public static final String HELP = "help";
    public static final String ANALYTICS = "analytics";
    public static final String CLEAR_CACHE = "clearCache";
    public static final String STREAM_CACHE_SIZE = "streamCacheSize";
    public static final String CLEAR_STREAM_CACHE = "clearStreamCache";
    public static final String WIRELESS = "wireless";
    public static final String EXTRAS = "extras";
    public static final String ACCOUNT_SYNC_SETTINGS = "accountSyncSettings";
    public static final String NOTIFICATION_SETTINGS = "notificationSettings";
    public static final String VERSION = "version";
    public static final int CLICKS_TO_DEBUG_MODE = 5;

    private int mClicksToDebug = CLICKS_TO_DEBUG_MODE;

    private ProgressDialog mDeleteDialog;
    private AccountOperations mAccountOperations;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        mAccountOperations = new AccountOperations(this);
        addPreferencesFromResource(R.xml.settings);

        PreferenceGroup extras = (PreferenceGroup) findPreference(EXTRAS);
        if (AlarmClock.isFeatureEnabled(this)) {
            AlarmClock.get(getApplicationContext()).addPrefs(this, extras);
        } else {
            getPreferenceScreen().removePreference(extras);
        }

        findPreference(ACCOUNT_SYNC_SETTINGS).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        getApp().track(Page.Settings_account_sync);
                        Intent intent = new Intent(Settings.this, AccountSettings.class);
                        startActivity(intent);
                        return true;
                    }
        });

        findPreference(NOTIFICATION_SETTINGS).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        getApp().track(Page.Settings_notifications);
                        Intent intent = new Intent(Settings.this, NotificationSettings.class);
                        startActivity(intent);
                        return true;
                    }
        });

        final ChangeLog cl = new ChangeLog(this);
        findPreference(CHANGE_LOG).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        getApp().track(Page.Settings_change_log);
                        cl.getDialog(true).show();
                        return true;
                    }
                });

        findPreference(LOGOUT).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        if (!AndroidUtils.isUserAMonkey()) {
                            // don't let the monkey log out
                            safeShowDialog(DIALOG_USER_LOGOUT_CONFIRM);
                        }
                        return true;
                    }
                });

        findPreference(HELP).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://help.soundcloud.com/"));
                        startActivity(browserIntent);
                        return true;
                    }
                });

        findPreference(CLEAR_CACHE).setOnPreferenceClickListener(
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

        findPreference(CLEAR_STREAM_CACHE).setOnPreferenceClickListener(
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


        findPreference(WIRELESS).setOnPreferenceClickListener(
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

        final Preference versionPref = findPreference(VERSION);
        versionPref.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        mClicksToDebug--;
                        if (mClicksToDebug == 0) {
                            togglePlaybackDebugMode();
                            mClicksToDebug = CLICKS_TO_DEBUG_MODE; // reset for another toggle
                        }
                        return true;
                    }
                });

        try {
            versionPref.setSummary(getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to get verion info ", e);
            versionPref.setSummary(getString(R.string.unavailable));
        }


        if (!SoundCloudApplication.DEV_MODE) {
            getPreferenceScreen().removePreference(findPreference(DevSettings.PREF_KEY));
        } else {
            DevSettings.setup(this, getApp());
        }
    }

    private void togglePlaybackDebugMode() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean enabled = !preferences.getBoolean(Consts.PrefKeys.PLAYBACK_ERROR_REPORTING_ENABLED, false);
        SharedPreferencesUtils.apply(preferences.edit().putBoolean(Consts.PrefKeys.PLAYBACK_ERROR_REPORTING_ENABLED, enabled));

        Log.d(CloudPlaybackService.TAG, "toggling error reporting (enabled=" + enabled + ")");
        AndroidUtils.showToast(this, getString(R.string.playback_error_logging, getText(enabled ? R.string.enabled : R.string.disabled)));

    }

    public void safeShowDialog(int dialogId) {
        if (!isFinishing()) {
            showDialog(dialogId);
        }
    }

    private void updateClearCacheTitles() {
        setClearCacheTitle(CLEAR_CACHE, R.string.pref_clear_cache, IOUtils.getCacheDir(this));
        setClearCacheTitle(CLEAR_STREAM_CACHE, R.string.pref_clear_stream_cache, Consts.EXTERNAL_STREAM_DIRECTORY);
    }

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    @Override
    protected void onResume() {
        getApp().track(Settings.class);
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

    @TargetApi(11)
    public static AlertDialog createLogoutDialog(Activity activity) {
        final SoundCloudApplication app = (SoundCloudApplication) activity.getApplication();
        final AccountOperations accountOperations = new AccountOperations(app);
        app.track(Click.Log_out_log_out);

        return new AlertDialog.Builder(activity).setTitle(R.string.menu_clear_user_title)
                .setMessage(R.string.menu_clear_user_desc)
                .setPositiveButton(android.R.string.ok,
                        new LogoutClickListener(activity))
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        app.track(Click.Log_out_box_cancel);
                    }
                })
                .create();
    }

    @NotNull
    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public int getMenuResourceId() {
        return R.menu.main;
    }

    @Override
    public void onHomePressed() {
    }

    private static class LogoutClickListener implements DialogInterface.OnClickListener {

        private final SoundCloudApplication mSoundCloudApplication;
        private final AccountOperations mAccountOperations;
        private final Activity mActivityContext;

        private LogoutClickListener(Activity activityContext) {
            mActivityContext = activityContext;
            mSoundCloudApplication = (SoundCloudApplication)activityContext.getApplicationContext();
            this.mAccountOperations = new AccountOperations(mSoundCloudApplication);
        }

        @Override
        public void onClick(final DialogInterface dialog, int whichButton) {
            final ProgressDialog progressDialog = AndroidUtils.showProgress(mActivityContext, R.string.settings_logging_out);

            mSoundCloudApplication.track(Click.Log_out_box_ok);
            mAccountOperations.removeSoundCloudAccount().subscribe(new ScObserver<Void>() {
                @Override
                public void onCompleted() {
                    mAccountOperations.addSoundCloudAccountManually(mActivityContext);
                    progressDialog.dismiss();
                    mActivityContext.finish();
                }

                @Override
                public void onError(Exception e) {
                    progressDialog.dismiss();
                    new AlertDialog.Builder(mActivityContext)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setMessage(R.string.settings_error_revoking_account_message)
                            .setPositiveButton(android.R.string.ok, null)
                            .create()
                            .show();
                }

            }, ScSchedulers.BACKGROUND_SCHEDULER);
        }
    };
}
