package com.soundcloud.android.service.beta;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.utils.CloudUtils.getAppVersion;
import static com.soundcloud.android.utils.CloudUtils.getAppVersionCode;
import static com.soundcloud.android.utils.CloudUtils.getElapsedTimeString;

import com.soundcloud.android.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BetaPreferences {

    public static void add(final Context context, PreferenceGroup group) {
        Preference beta = new Preference(context);
        beta.setTitle(R.string.pref_beta);
        beta.setSummary(R.string.pref_beta_summary);
        beta.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final Content content = BetaService.getMostRecentContent();
                String message = "";
                if (content != null) {
                    if (content.isUptodate(context)) {
                        message += "Your beta version is up to date. ";
                    } else {
                        message += String.format("Last downloaded beta: %s, version: %s (%d), updated %s. ",
                                getElapsedTimeString(context.getResources(), content.downloadTime()),
                                content.getVersionName(),
                                content.getVersionCode(),
                                getElapsedTimeString(context.getResources(), content.lastmodified)
                        );
                    }
                } else {
                    message += "No beta downloaded yet. ";
                }

                message += String.format("Installed version: %s (%d)",
                        getAppVersion(context, "unknown"),
                        getAppVersionCode(context, -1));

                AlertDialog.Builder b = new AlertDialog.Builder(context)
                        .setTitle(R.string.pref_beta)
                        .setMessage(message)
                        .setPositiveButton(R.string.pref_beta_check_check_now, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (BetaService.checkNow(context)) {
                                    Toast.makeText(context, R.string.pref_beta_check_checking_now, Toast.LENGTH_SHORT)
                                         .show();
                                }
                            }
                        });

                if (content != null && !content.isUptodate(context)) {
                    b.setNeutralButton(R.string.pref_beta_install_now, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            context.startActivity(content.getInstallIntent());
                        }
                    });
                }
                b.create().show();
                return true;
            }
        });

        CheckBoxPreference autoUpdate = new CheckBoxPreference(context);
        autoUpdate.setTitle(R.string.pref_beta_check_for_updates);
        autoUpdate.setSummary(
                context.getString(R.string.pref_beta_check_for_updates_summary,
                        BetaService.INTERVAL/60/1000));
        autoUpdate.setKey(BetaService.PREF_CHECK_UPDATES);
        autoUpdate.setDefaultValue(true);

        group.addPreference(beta);
        group.addPreference(autoUpdate);
        /*
        // does not work - android styling bug

        PreferenceScreen screen = preferenceScreenFromResource(R.xml.settings_beta);
        if (screen != null) {
            getPreferenceScreen().addPreference(screen);
        }
        */
    }

    /** @noinspection UnusedDeclaration*/
    private static PreferenceScreen
        // hacky way to use package private method on PreferenceManager

        preferenceScreenFromResource(PreferenceManager pm, Context ctxt, int settings_beta) {
        try {
            Class pmClass = pm.getClass();

            Method m = pmClass.getDeclaredMethod("inflateFromResource", Context.class
                    , Integer.TYPE, PreferenceScreen.class);
            m.setAccessible(true);
            return (PreferenceScreen) m.invoke(pm, ctxt, settings_beta, null);
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
}
