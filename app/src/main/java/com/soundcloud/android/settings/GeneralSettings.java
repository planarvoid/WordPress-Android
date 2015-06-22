package com.soundcloud.android.settings;

import static android.preference.Preference.OnPreferenceClickListener;
import static android.provider.Settings.ACTION_WIRELESS_SETTINGS;
import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.settings.SettingKey.CLEAR_CACHE;
import static com.soundcloud.android.settings.SettingKey.GENERAL_SETTINGS;
import static com.soundcloud.android.settings.SettingKey.HELP;
import static com.soundcloud.android.settings.SettingKey.LEGAL;
import static com.soundcloud.android.settings.SettingKey.LOGOUT;
import static com.soundcloud.android.settings.SettingKey.NOTIFICATION_SETTINGS;
import static com.soundcloud.android.settings.SettingKey.OFFLINE_SYNC_SETTINGS;
import static com.soundcloud.android.settings.SettingKey.VERSION;
import static com.soundcloud.android.settings.SettingKey.WIRELESS;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.LogoutActivity;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.Log;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;

import javax.inject.Inject;

class GeneralSettings implements OnPreferenceClickListener {

    public static final int CLICKS_TO_DEBUG_MODE = 5;
    private int clicksToDebug = CLICKS_TO_DEBUG_MODE;

    private final Context appContext;
    private final DeviceHelper deviceHelper;
    private final FeatureOperations featureOperations;

    private PreferenceFragment settings;

    @Inject
    public GeneralSettings(Context appContext, DeviceHelper deviceHelper, FeatureOperations featureOperations) {
        this.appContext = appContext;
        this.deviceHelper = deviceHelper;
        this.featureOperations = featureOperations;
    }

    public void addTo(final PreferenceFragment settings) {
        this.settings = settings;
        settings.addPreferencesFromResource(R.xml.settings_general);
        setupOfflineSync(settings);
        setupListeners(settings);
        setupVersion(settings);
    }

    private void setupOfflineSync(PreferenceFragment settings) {
        /*
         * TODO: This should also check whether there is offline content to remove.
         *
         * The offline settings screen has a configuration for the case where offline content is not enabled
         * but there is offline content stored on the device which can be deleted.
         */
        if (featureOperations.isOfflineContentEnabled() || featureOperations.upsellMidTier()) {
            final PreferenceCategory category = (PreferenceCategory) settings.findPreference(GENERAL_SETTINGS);
            category.addPreference(createOfflineSyncPref(settings));
        }
    }

    private Preference createOfflineSyncPref(final PreferenceFragment settings) {
        final Activity parent = settings.getActivity();
        Preference offlineSettings = new Preference(parent);
        offlineSettings.setKey(OFFLINE_SYNC_SETTINGS);
        offlineSettings.setTitle(R.string.pref_offline_settings);
        offlineSettings.setSummary(R.string.pref_offline_settings_summary);
        offlineSettings.setOrder(1);
        offlineSettings.setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(parent, OfflineSettingsActivity.class);
                        parent.startActivity(intent);
                        return true;
                    }
                }
        );
        return offlineSettings;
    }

    private void setupVersion(PreferenceFragment settings) {
        final Preference versionPref = settings.findPreference(VERSION);
        versionPref.setSummary(deviceHelper.getUserVisibleVersion());
        versionPref.setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
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

    private void setupListeners(final PreferenceFragment settings) {
        settings.findPreference(NOTIFICATION_SETTINGS).setOnPreferenceClickListener(this);
        settings.findPreference(LEGAL).setOnPreferenceClickListener(this);
        settings.findPreference(LOGOUT).setOnPreferenceClickListener(this);
        settings.findPreference(HELP).setOnPreferenceClickListener(this);
        settings.findPreference(CLEAR_CACHE).setOnPreferenceClickListener(this);
        settings.findPreference(WIRELESS).setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        final Activity parent = settings.getActivity();
        switch (preference.getKey()) {
            case NOTIFICATION_SETTINGS:
                parent.startActivity(new Intent(parent, NotificationSettingsActivity.class));
                return true;
            case LEGAL:
                parent.startActivity(new Intent(parent, LegalActivity.class));
                return true;
            case LOGOUT:
                if (!AndroidUtils.isUserAMonkey()) { // Don't let the monkey sign out
                    showLogoutDialog(parent);
                }
                return true;
            case HELP:
                parent.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(appContext.getString(R.string.url_support))));
                return true;
            case CLEAR_CACHE:
                ClearCacheDialog.show(parent.getFragmentManager());
                return true;
            case WIRELESS:
                try {
                    parent.startActivity(new Intent(ACTION_WIRELESS_SETTINGS));
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "Device does not have WiFi settings", e);
                }
            default:
                return false;
        }
    }

    private void showLogoutDialog(final Activity parent) {
        new AlertDialog.Builder(parent)
                .setTitle(R.string.menu_clear_user_title)
                .setMessage(R.string.menu_clear_user_desc)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogoutActivity.start(parent);
                    }
                }).show();
    }

    private void togglePlaybackDebugMode() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        final boolean enabled = !preferences.getBoolean(Consts.PrefKeys.PLAYBACK_ERROR_REPORTING_ENABLED, false);
        preferences.edit().putBoolean(Consts.PrefKeys.PLAYBACK_ERROR_REPORTING_ENABLED, enabled).apply();

        Log.d(PlaybackService.TAG, "Toggling error reporting (enabled=" + enabled + ")");
        Resources resources = appContext.getResources();
        AndroidUtils.showToast(appContext, resources.getString(R.string.playback_error_logging, resources.getText(enabled ? R.string.enabled : R.string.disabled)));
    }

}
