package com.soundcloud.android.settings;

import static android.provider.Settings.ACTION_WIRELESS_SETTINGS;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.properties.ApplicationProperties;
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
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

public class GeneralSettings {

    // TODO: Currently these constants must match the values in keys_settings.xml - refactor!
    public static final String LOGOUT = "logout";
    public static final String HELP = "help";
    public static final String ANALYTICS_ENABLED = "analytics_enabled";
    public static final String CLEAR_CACHE = "clearCache";
    public static final String WIRELESS = "wireless";
    public static final String GENERAL_SETTINGS = "generalSettings";
    public static final String OFFLINE_SYNC_SETTINGS = "offlineSyncSettings";
    public static final String ACCOUNT_SYNC_SETTINGS = "accountSyncSettings";
    public static final String NOTIFICATION_SETTINGS = "notificationSettings";
    public static final String LEGAL = "legal";
    public static final String VERSION = "version";
    public static final String FORCE_SKIPPY = "forceSkippy";
    public static final String CRASH_REPORTING_ENABLED = "acra.enable";

    public static final int CLICKS_TO_DEBUG_MODE = 5;

    private int clicksToDebug = CLICKS_TO_DEBUG_MODE;

    private final DeviceHelper deviceHelper;
    private final ImageOperations imageOperations;
    private final Resources resources;
    private final Context appContext;
    private final ApplicationProperties applicationProperties;

    @Inject
    public GeneralSettings(Context appContext, Resources resources, DeviceHelper deviceHelper, ImageOperations imageOperations, ApplicationProperties applicationProperties) {
        this.appContext = appContext;
        this.resources = resources;
        this.deviceHelper = deviceHelper;
        this.imageOperations = imageOperations;
        this.applicationProperties = applicationProperties;
    }

    public void setup(final SettingsActivity activity) {
        activity.addPreferencesFromResource(R.xml.settings_general);
        removeOfflineSync(activity);
        setupListeners(activity);
        setupVersion(activity);
    }

    private void removeOfflineSync(final SettingsActivity activity) {
        if (!applicationProperties.isAlphaBuild() && !applicationProperties.isDebugBuild()) {
            final PreferenceCategory category = (PreferenceCategory) activity.findPreference(GENERAL_SETTINGS);
            category.removePreference(activity.findPreference(OFFLINE_SYNC_SETTINGS));
        }
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
        activity.findPreference(OFFLINE_SYNC_SETTINGS).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(activity, OfflineSettingsActivity.class);
                        activity.startActivity(intent);
                        return true;
                    }
                }
        );

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
                        activity.safeShowDialog(SettingsActivity.DIALOG_CACHE_DELETING);

                        final WeakReference<SettingsActivity> weakReference = new WeakReference<>(activity);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                imageOperations.clearDiskCache();
                                IOUtils.cleanDirs(Consts.EXTERNAL_MEDIAPLAYER_STREAM_DIRECTORY, Consts.EXTERNAL_SKIPPY_STREAM_DIRECTORY);
                                updateActivityIfNeeded(weakReference);
                            }
                        }).start();
                        return true;
                    }

                    private void updateActivityIfNeeded(WeakReference<SettingsActivity> weakReference) {
                        final SettingsActivity settingsActivity = weakReference.get();
                        if (settingsActivity != null && !settingsActivity.isFinishing()) {
                            settingsActivity.dismissDialog(SettingsActivity.DIALOG_CACHE_DELETING);
                        }
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

    private void togglePlaybackDebugMode() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        final boolean enabled = !preferences.getBoolean(Consts.PrefKeys.PLAYBACK_ERROR_REPORTING_ENABLED, false);
        preferences.edit().putBoolean(Consts.PrefKeys.PLAYBACK_ERROR_REPORTING_ENABLED, enabled).apply();

        Log.d(PlaybackService.TAG, "toggling error reporting (enabled=" + enabled + ")");
        AndroidUtils.showToast(appContext, resources.getString(R.string.playback_error_logging, resources.getText(enabled ? R.string.enabled : R.string.disabled)));
    }

}
