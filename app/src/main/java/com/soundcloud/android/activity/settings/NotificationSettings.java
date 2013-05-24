package com.soundcloud.android.activity.settings;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.service.sync.SyncConfig;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;

import android.accounts.Account;
import android.content.ContentResolver;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;

import java.util.ArrayList;
import java.util.List;

@Tracking(page = Page.Settings_notifications)
public class NotificationSettings extends PreferenceActivity {
    final List<CheckBoxPreference> syncPreferences = new ArrayList<CheckBoxPreference>();
    private AccountOperations accountOperations;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountOperations = new AccountOperations(this);
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
        ((SoundCloudApplication) getApplication()).track(getClass());
    }

    private boolean checkSyncNecessary() {
        boolean sync = false;
        for (CheckBoxPreference p : syncPreferences) {
            if (p.isChecked()) {
                sync = true;
                break;
            }
        }
        if (accountOperations.soundCloudAccountExists()) {
            final Account account = accountOperations.getSoundCloudAccount();
            final boolean autoSyncing = ContentResolver.getSyncAutomatically(account, ScContentProvider.AUTHORITY);
            if (sync && !autoSyncing) {
                ScContentProvider.enableSyncing(account, SyncConfig.DEFAULT_SYNC_DELAY);
            } else if (!sync && autoSyncing) {
                ScContentProvider.disableSyncing(account);
            }
        }
        return sync;
    }
}
