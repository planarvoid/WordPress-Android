package com.soundcloud.android.settings;

import static android.provider.Settings.ACTION_WIRELESS_SETTINGS;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.cache.FileCache;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import javax.inject.Inject;
import java.io.File;
import java.lang.ref.WeakReference;

public class GeneralSettings {

    // TODO: Currently these constants must match the values in settings_keys.xml - refactor!
    public static final String LOGOUT = "logout";
    public static final String HELP = "help";
    public static final String ANALYTICS_ENABLED = "analytics_enabled";
    public static final String CLEAR_CACHE = "clearCache";
    public static final String STREAM_CACHE_SIZE = "streamCacheSize";
    public static final String CLEAR_STREAM_CACHE = "clearStreamCache";
    public static final String WIRELESS = "wireless";
    public static final String ACCOUNT_SYNC_SETTINGS = "accountSyncSettings";
    public static final String NOTIFICATION_SETTINGS = "notificationSettings";
    public static final String LEGAL = "legal";
    public static final String VERSION = "version";
    public static final String FORCE_SKIPPY = "forceSkippy";

    public static final int CLICKS_TO_DEBUG_MODE = 5;
    public static final String CRASH_REPORTING_ENABLED = "acra.enable";

    private int clicksToDebug = CLICKS_TO_DEBUG_MODE;

    private final DeviceHelper deviceHelper;
    private final ImageOperations imageOperations;
    private final Resources resources;
    private final Context appContext;

    @Inject
    public GeneralSettings(Context appContext, Resources resources, DeviceHelper deviceHelper, ImageOperations imageOperations) {
        this.appContext = appContext;
        this.resources = resources;
        this.deviceHelper = deviceHelper;
        this.imageOperations = imageOperations;
    }

    public void setup(final SettingsActivity activity) {
        activity.addPreferencesFromResource(R.xml.settings_general);
        setupListeners(activity);
        setupVersion(activity);
    }

    private void setupVersion(SettingsActivity activity) {
        final Preference versionPref = activity.findPreference(VERSION);
        versionPref.setSummary(deviceHelper.getUserVisibleVersion());
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
    }

    @SuppressWarnings({"PMD.ExcessiveMethodLength", "PMD.ModifiedCyclomaticComplexity"})
    private void setupListeners(final SettingsActivity activity) {
        activity.findPreference(ACCOUNT_SYNC_SETTINGS).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(activity, AccountSettingsActivity.class);
                        activity.startActivity(intent);
                        return true;
                    }
                }
        );

        activity.findPreference(NOTIFICATION_SETTINGS).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(activity, NotificationSettingsActivity.class);
                        activity.startActivity(intent);
                        return true;
                    }
                }
        );

        activity.findPreference(LEGAL).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(activity, LegalActivity.class);
                        activity.startActivity(intent);
                        return true;
                    }
                }
        );

        activity.findPreference(LOGOUT).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        if (!AndroidUtils.isUserAMonkey()) {
                            // don't let the monkey log out
                            activity.safeShowDialog(SettingsActivity.DIALOG_USER_LOGOUT_CONFIRM);
                        }
                        return true;
                    }
                }
        );

        activity.findPreference(HELP).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        Intent supportIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(appContext.getString(R.string.url_support)));
                        activity.startActivity(supportIntent);
                        return true;
                    }
                }
        );

        activity.findPreference(CLEAR_CACHE).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        activity.safeShowDialog(SettingsActivity.DIALOG_IMAGE_CACHE_DELETING);

                        final Handler handler = new Handler();
                        final WeakReference<SettingsActivity> weakReference = new WeakReference<>(activity);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                imageOperations.clearDiskCache();
                                updateActivityIfNeeded(handler, weakReference);
                            }
                        }).start();
                        return true;
                    }

                    private void updateActivityIfNeeded(Handler handler, WeakReference<SettingsActivity> weakReference) {
                        final SettingsActivity settingsActivity = weakReference.get();
                        if (settingsActivity != null && !settingsActivity.isFinishing()) {
                            updateImageCacheSizeSync(handler, settingsActivity);
                            settingsActivity.dismissDialog(SettingsActivity.DIALOG_IMAGE_CACHE_DELETING);
                        }
                    }
                }
        );

        activity.findPreference(CLEAR_STREAM_CACHE).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        getDeleteCacheTask(activity)
                                .execute(Consts.EXTERNAL_MEDIAPLAYER_STREAM_DIRECTORY, Consts.EXTERNAL_SKIPPY_STREAM_DIRECTORY);
                        return true;
                    }
                }
        );


        activity.findPreference(WIRELESS).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        try { // rare phones have no wifi settings
                            activity.startActivity(new Intent(ACTION_WIRELESS_SETTINGS));
                        } catch (ActivityNotFoundException e) {
                            Log.e(TAG, "error", e);
                        }
                        return true;
                    }
                }
        );
    }

    private FileCache.CleanCacheTask getDeleteCacheTask(final SettingsActivity activity) {
        return new FileCache.CleanCacheTask(true) {
            @Override
            protected void onPreExecute() {
                activity.safeShowDialog(SettingsActivity.DIALOG_STREAM_CACHE_DELETING);
            }

            @Override
            protected void onProgressUpdate(Integer... progress) {
                activity.updateDeleteDialog(progress[0], progress[1]);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                activity.removeDialog(SettingsActivity.DIALOG_STREAM_CACHE_DELETING);
                updateClearCacheTitles(activity);
            }
        };
    }

    private void togglePlaybackDebugMode() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        final boolean enabled = !preferences.getBoolean(Consts.PrefKeys.PLAYBACK_ERROR_REPORTING_ENABLED, false);
        preferences.edit().putBoolean(Consts.PrefKeys.PLAYBACK_ERROR_REPORTING_ENABLED, enabled).apply();

        Log.d(PlaybackService.TAG, "toggling error reporting (enabled=" + enabled + ")");
        AndroidUtils.showToast(appContext, resources.getString(R.string.playback_error_logging, resources.getText(enabled ? R.string.enabled : R.string.disabled)));
    }

    void updateClearCacheTitles(PreferenceActivity activity) {
        setClearCacheTitle(activity, CLEAR_CACHE, R.string.pref_clear_cache, imageOperations.getDiskCacheDirectory());
        setClearCacheTitle(activity, CLEAR_STREAM_CACHE, R.string.pref_clear_stream_cache,
                Consts.EXTERNAL_MEDIAPLAYER_STREAM_DIRECTORY, Consts.EXTERNAL_SKIPPY_STREAM_DIRECTORY);
    }

    private void updateImageCacheSizeSync(Handler handler, SettingsActivity activity) {
        setClearCacheTitleSync(handler, activity, CLEAR_CACHE, R.string.pref_clear_cache, imageOperations.getDiskCacheDirectory());
    }

    private void setClearCacheTitle(final PreferenceActivity activity, final String pref, final int key, final File... directories) {
        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                setClearCacheTitleSync(handler, activity, pref, key, directories);
            }
        }.start();
    }

    private void setClearCacheTitleSync(Handler handler, final PreferenceActivity activity, final String pref, final int key, File... directories) {
        final String size = IOUtils.inMbFormatted(directories);
        handler.post(new Runnable() {
            @Override
            public void run() {
                activity.findPreference(pref).setTitle(resources.getString(key) + " [" + size + " MB]");
            }
        });
    }

}
