package com.soundcloud.android.service.beta;

import static com.soundcloud.android.utils.CloudUtils.getAppVersion;
import static com.soundcloud.android.utils.CloudUtils.getAppVersionCode;
import static com.soundcloud.android.utils.CloudUtils.getElapsedTimeString;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.widget.Toast;

public class BetaPreferences {

    public static void add(final Context context, PreferenceGroup group) {
        PreferenceCategory cat = new PreferenceCategory(context);
        cat.setTitle(R.string.pref_beta_title);

        Preference beta = new Preference(context);
        beta.setTitle(R.string.pref_beta);
        beta.setSummary(BetaService.isUptodate(context)
                ? R.string.pref_beta_summary : R.string.pref_beta_summary_outdated);

        beta.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                getUpdateDialog(context, BetaService.getMostRecentContent()).show();
                return true;
            }
        });

        CheckBoxPreference autoUpdate = new CheckBoxPreference(context);
        autoUpdate.setTitle(R.string.pref_beta_check_for_updates);
        autoUpdate.setSummary(
                context.getString(R.string.pref_beta_check_for_updates_summary,
                        context.getString(getResourceForInterval((int) BetaService.INTERVAL))));

        autoUpdate.setKey(Consts.PrefKeys.BETA_CHECK_FOR_UPDATES);
        autoUpdate.setDefaultValue(true);

        CheckBoxPreference requireWifi = new CheckBoxPreference(context);
        requireWifi.setTitle(R.string.pref_beta_require_wifi);
        requireWifi.setSummary(R.string.pref_beta_require_wifi_summary);
        requireWifi.setDefaultValue(true);
        requireWifi.setKey(Consts.PrefKeys.BETA_REQUIRE_WIFI);

        group.addPreference(cat);

        cat.addPreference(beta);
        cat.addPreference(autoUpdate);
        cat.addPreference(requireWifi);
        /*
        // does not work - android styling bug

        PreferenceScreen screen = preferenceScreenFromResource(R.xml.settings_beta);
        if (screen != null) {
            getPreferenceScreen().addPreference(screen);
        }
        */
    }

    private static int getResourceForInterval(int interval) {
        switch (interval) {
            case (int) AlarmManager.INTERVAL_FIFTEEN_MINUTES: return R.string.pref_beta_interval_900;
            case (int) AlarmManager.INTERVAL_HALF_HOUR: return R.string.pref_beta_interval_1800;
            case (int) AlarmManager.INTERVAL_HOUR: return R.string.pref_beta_interval_3600;
            case (int) AlarmManager.INTERVAL_HALF_DAY: return R.string.pref_beta_interval_43200;
            case (int) AlarmManager.INTERVAL_DAY: return R.string.pref_beta_interval_86400;
            default:
                return R.string.pref_beta_interval_unknown;

        }
    }

    public static Dialog getUpdateDialog(final Context context, final Beta beta) {
        String message = "";
        if (beta != null) {
            if (beta.isInstalled(context)) {
                message += "Your beta version is up to date. ";
            } else {
                message += String.format("Last downloaded beta:\n%s, version: %s (%d),\nupdated %s. ",
                        getElapsedTimeString(context.getResources(), beta.downloadTime(), true),
                        beta.getVersionName(),
                        beta.getVersionCode(),
                        getElapsedTimeString(context.getResources(), beta.lastmodified, true)
                );
            }
        } else {
            message += "No beta downloaded yet. ";
        }

        message += String.format("Installed version:\n%s (%d)",
                getAppVersion(context, "unknown"),
                getAppVersionCode(context, -1));

        AlertDialog.Builder b = new AlertDialog.Builder(context)
                .setTitle(R.string.pref_beta)
                .setMessage(message)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton(R.string.pref_beta_check_check_now, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (BetaService.checkNow(context)) {
                            Toast.makeText(context, R.string.pref_beta_check_checking_now, Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });

        if (beta != null && !beta.isInstalled(context)) {
            b.setNeutralButton(R.string.pref_beta_install_now, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    context.startActivity(beta.getInstallIntent());
                }
            });
        }
        return b.create();
    }
}
