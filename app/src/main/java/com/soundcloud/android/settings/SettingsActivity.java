package com.soundcloud.android.settings;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.LogoutActivity;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.properties.FeatureFlags;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import javax.inject.Inject;

public class SettingsActivity extends ScSettingsActivity {

    static final int DIALOG_USER_LOGOUT_CONFIRM = 0;
    static final int DIALOG_CACHE_DELETING = 1;

    private ProgressDialog deleteDialog;

    @Inject ApplicationProperties applicationProperties;
    @Inject GeneralSettings generalSettings;
    @Inject DeveloperSettings developerSettings;
    @Inject FeatureFlags featureFlags;

    public SettingsActivity() {}

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        if (featureFlags.isEnabled(Flag.PAYMENTS)) {
            addPreferencesFromResource(R.xml.settings_subscriptions);
        }

        generalSettings.setup(this);

        if (!applicationProperties.isReleaseBuild()) {
            addPreferencesFromResource(R.xml.settings_extras);
        }

        if (applicationProperties.isDebugBuild()) {
            developerSettings.setup(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SETTINGS_MAIN));
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_CACHE_DELETING:
                if (deleteDialog == null) {
                    deleteDialog = new ProgressDialog(this);
                    deleteDialog.setTitle(R.string.cache_clearing);
                    deleteDialog.setMessage(getString(R.string.cache_clearing_message));
                    deleteDialog.setIndeterminate(true);
                    deleteDialog.setCancelable(false);
                }
                return deleteDialog;
            case DIALOG_USER_LOGOUT_CONFIRM:
                return createLogoutDialog();
        }
        return super.onCreateDialog(id);
    }

    void safeShowDialog(int dialogId) {
        if (!isFinishing()) {
            showDialog(dialogId);
        }
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
