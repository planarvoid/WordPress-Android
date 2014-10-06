package com.soundcloud.android.preferences;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.soundcloud.android.sync.SyncConfig;

import android.accounts.Account;
import android.content.ContentResolver;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;

import java.util.ArrayList;
import java.util.List;

public class NotificationSettingsActivity extends ScSettingsActivity {

    final List<CheckBoxPreference> syncPreferences = new ArrayList<CheckBoxPreference>();
    private AccountOperations accountOperations;

    public NotificationSettingsActivity() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountOperations = SoundCloudApplication.fromContext(this).getAccountOperations();
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

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SETTINGS_NOTIFICATIONS));
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
