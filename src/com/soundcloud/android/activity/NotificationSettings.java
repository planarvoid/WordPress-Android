package com.soundcloud.android.activity;

import android.preference.*;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.ScContentProvider;

import android.accounts.Account;
import android.content.ContentResolver;
import android.os.Bundle;

import com.soundcloud.android.service.sync.SyncConfig;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;

import java.util.ArrayList;
import java.util.List;

@Tracking(page = Page.Settings_notifications)
public class NotificationSettings extends PreferenceActivity {
    final List<CheckBoxPreference> syncPreferences = new ArrayList<CheckBoxPreference>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.notifications_settings);

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

    @Override
    protected void onResume() {
        super.onResume();
        ((SoundCloudApplication)getApplication()).track(getClass());
    }

    private boolean checkSyncNecessary() {
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
                ScContentProvider.enableSyncing(account, SyncConfig.DEFAULT_SYNC_DELAY);
            } else if (!sync && autoSyncing){
                ScContentProvider.disableSyncing(account);
            }
        }
        return sync;
    }
}
