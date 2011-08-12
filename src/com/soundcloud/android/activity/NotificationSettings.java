package com.soundcloud.android.activity;

import android.accounts.Account;
import android.content.ContentResolver;
import android.nfc.tech.IsoDep;
import android.preference.*;
import android.util.Log;
import com.soundcloud.android.R;

import android.os.Bundle;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.task.NewConnectionTask;

import java.util.ArrayList;

public class NotificationSettings extends PreferenceActivity {

    ArrayList<CheckBoxPreference> syncPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.notifications_settings);

        syncPreferences = new ArrayList<CheckBoxPreference>();
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            if (getPreferenceScreen().getPreference(i) instanceof PreferenceCategory) {

                final PreferenceCategory preferenceCategory = (PreferenceCategory) getPreferenceScreen().getPreference(i);
                for (int j = 0; j < preferenceCategory.getPreferenceCount(); j++) {
                    if (preferenceCategory.getPreference(j) instanceof CheckBoxPreference) {

                        final CheckBoxPreference cp = (CheckBoxPreference) preferenceCategory.getPreference(j);
                        syncPreferences.add(cp);
                        cp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                checkSyncNecessary();
                                return false;
                            }
                        });
                    }

                }
            }
        }
    }

    private void checkSyncNecessary() {
        PreferenceManager.getDefaultSharedPreferences(this);
        boolean sync = false;
        for (CheckBoxPreference p : syncPreferences) {
            if (p.isChecked()) {
                sync = true;
                break;
            }
        }

        final Account account = ((SoundCloudApplication) getApplication()).getAccount();
        if (account != null) {
            final boolean autoSyncing = ContentResolver.getSyncAutomatically(account, ScContentProvider.AUTHORITY);
            if (sync && !autoSyncing) {
                ScContentProvider.enableSyncing(account);
            } else if (!sync && autoSyncing){
                ScContentProvider.disableSyncing(account);
            }
        }

    }
}
