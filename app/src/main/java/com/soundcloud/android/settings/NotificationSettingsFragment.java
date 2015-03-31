package com.soundcloud.android.settings;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.soundcloud.android.sync.SyncConfig;

import android.accounts.Account;
import android.content.ContentResolver;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class NotificationSettingsFragment extends PreferenceFragment {

    @Inject AccountOperations accountOperations;

    final List<CheckBoxPreference> syncPreferences = new ArrayList<>();

    public static NotificationSettingsFragment create() {
        return new NotificationSettingsFragment();
    }

    public NotificationSettingsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_notifications);

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

    private boolean checkSyncNecessary() {
        boolean sync = false;
        for (CheckBoxPreference p : syncPreferences) {
            if (p.isChecked()) {
                sync = true;
                break;
            }
        }
        if (accountOperations.isUserLoggedIn()) {
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
