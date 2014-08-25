package com.soundcloud.android.preferences;

import static android.provider.Settings.ACTION_WIRELESS_SETTINGS;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.LogoutActivity;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.cache.FileCache;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;

import android.annotation.TargetApi;
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
import android.preference.PreferenceManager;

import javax.inject.Inject;
import java.io.File;

public class SettingsActivity extends ScSettingsActivity {

    private static final int DIALOG_CACHE_DELETING = 0;
    private static final int DIALOG_USER_LOGOUT_CONFIRM = 1;

    //IMPORTANT make sure these match the values in settings.xml
    public static final String LOGOUT = "logout";
    public static final String HELP = "help";
    public static final String ANALYTICS_ENABLED = "analytics_enabled";
    public static final String CLEAR_CACHE = "clearCache";
    public static final String STREAM_CACHE_SIZE = "streamCacheSize";
    public static final String CLEAR_STREAM_CACHE = "clearStreamCache";
    public static final String WIRELESS = "wireless";
    public static final String EXTRAS = "extras";
    public static final String ACCOUNT_SYNC_SETTINGS = "accountSyncSettings";
    public static final String NOTIFICATION_SETTINGS = "notificationSettings";
    public static final String LEGAL = "legal";
    public static final String VERSION = "version";
    public static final String FORCE_SKIPPY = "forceSkippy";
    public static final int CLICKS_TO_DEBUG_MODE = 5;

    public static final String CRASH_REPORTING_ENABLED = "acra.enable";

    private int clicksToDebug = CLICKS_TO_DEBUG_MODE;

    private ProgressDialog deleteDialog;
    @Inject ApplicationProperties applicationProperties;
    @Inject DeveloperPreferences developerPreferences;

    public SettingsActivity() {}

    @VisibleForTesting
    SettingsActivity(ApplicationProperties applicationProperties, EventBus eventBus, DeveloperPreferences developerPreferences) {
        super(eventBus);
        this.applicationProperties = applicationProperties;
        this.developerPreferences = developerPreferences;
    }

    @SuppressWarnings({"PMD.ExcessiveMethodLength"})
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.settings);

        findPreference(ACCOUNT_SYNC_SETTINGS).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(SettingsActivity.this, AccountSettingsActivity.class);
                        startActivity(intent);
                        return true;
                    }
                });

        findPreference(NOTIFICATION_SETTINGS).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(SettingsActivity.this, NotificationSettingsActivity.class);
                        startActivity(intent);
                        return true;
                    }
                });

        findPreference(LEGAL).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(SettingsActivity.this, LegalActivity.class);
                        startActivity(intent);
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
                                if (deleteDialog != null) {
                                    deleteDialog.setIndeterminate(false);
                                    deleteDialog.setProgress(progress[0]);
                                    deleteDialog.setMax(progress[1]);
                                }
                            }

                            @Override
                            protected void onPostExecute(Boolean result) {
                                removeDialog(DIALOG_CACHE_DELETING);
                                updateClearCacheTitles();
                            }
                        }.execute(IOUtils.getCacheDir(SettingsActivity.this));
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
                                if (deleteDialog != null) {
                                    deleteDialog.setIndeterminate(false);
                                    deleteDialog.setProgress(progress[0]);
                                    deleteDialog.setMax(progress[1]);
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
                        clicksToDebug--;
                        if (clicksToDebug == 0) {
                            togglePlaybackDebugMode();
                            clicksToDebug = CLICKS_TO_DEBUG_MODE; // reset for another toggle
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

        if (applicationProperties.isDebugBuild()) {
            developerPreferences.setup(this);
        } else {
            getPreferenceScreen().removePreference(findPreference(DeveloperPreferences.PREF_KEY));
            if (applicationProperties.isReleaseBuild()){
                getPreferenceScreen().removePreference(findPreference(EXTRAS));
            }
        }
    }

    private void togglePlaybackDebugMode() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean enabled = !preferences.getBoolean(Consts.PrefKeys.PLAYBACK_ERROR_REPORTING_ENABLED, false);
        preferences.edit().putBoolean(Consts.PrefKeys.PLAYBACK_ERROR_REPORTING_ENABLED, enabled).apply();

        Log.d(PlaybackService.TAG, "toggling error reporting (enabled=" + enabled + ")");
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

    @Override
    protected void onResume() {
        updateClearCacheTitles();
        super.onResume();
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SETTINGS_MAIN.get());
        }
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
                if (deleteDialog == null) {
                    deleteDialog = new ProgressDialog(this);
                    deleteDialog.setTitle(R.string.cache_clearing);
                    deleteDialog.setMessage(getString(R.string.cache_clearing_message));
                    deleteDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    deleteDialog.setIndeterminate(true);
                    deleteDialog.setCancelable(false);
                }
                return deleteDialog;

            case DIALOG_USER_LOGOUT_CONFIRM:
                return createLogoutDialog();
        }
        return super.onCreateDialog(id);
    }

    @TargetApi(11)
    private AlertDialog createLogoutDialog() {
        return new AlertDialog.Builder(this).setTitle(R.string.menu_clear_user_title)
                .setMessage(R.string.menu_clear_user_desc)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogoutActivity.start(SettingsActivity.this);
                    }
                }).create();
    }

    @Override
    public boolean onNavigateUp() {
        startActivity(new Intent(Actions.STREAM).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
        return true;
    }

}
