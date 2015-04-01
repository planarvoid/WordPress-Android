package com.soundcloud.android.settings;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;

import javax.inject.Inject;

public class OfflineSettingsActivity extends ScSettingsActivity {
    private static final String WIFI_ONLY = "offline.wifiOnlySync";
    private static final String OFFLINE_STORAGE_LIMIT = "offline.storageLimit";
    private static final String OFFLINE_REMOVE_ALL_OFFLINE_CONTENT = "offline.removeAllOfflineContent";

    private Subscription subscription = Subscriptions.empty();

    @Inject OfflineSettingsStorage offlineSettings;
    @Inject OfflineUsage offlineUsage;

    private final class CurrentDownloadSubscriber extends DefaultSubscriber<CurrentDownloadEvent> {
        @Override
        public void onNext(final CurrentDownloadEvent event) {
                ((OfflineStoragePreference) findPreference(OFFLINE_STORAGE_LIMIT)).updateAndRefresh();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_offline);
        subscription = eventBus.subscribe(EventQueue.CURRENT_DOWNLOAD, new CurrentDownloadSubscriber());
        setup();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SETTINGS_OFFLINE));
        }
    }

    @Override
    protected void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    private void setup() {
        setupWifiOnly();
        setupStorageLimit();
        setupRemoveAllOfflineContent();
    }

    private void setupWifiOnly() {
        final CheckBoxPreference featurePref = (CheckBoxPreference) findPreference(WIFI_ONLY);
        featurePref.setChecked(offlineSettings.isWifiOnlyEnabled());
        featurePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                offlineSettings.setWifiOnlyEnabled((boolean) o);
                return true;
            }
        });
    }

    private void setupStorageLimit() {
        final OfflineStoragePreference featurePref = (OfflineStoragePreference) findPreference(OFFLINE_STORAGE_LIMIT);

        featurePref.setOnStorageLimitChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                offlineSettings.setStorageLimit((long) o);
                return true;
            }
        });

        featurePref.setOfflineUsage(offlineUsage);
    }

    private void setupRemoveAllOfflineContent() {
        final Preference featurePref = findPreference(OFFLINE_REMOVE_ALL_OFFLINE_CONTENT);

        featurePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!isFinishing()) {
                    showRemoveAllOfflineContentDialog();
                }
                return true;
            }
        });
    }

    private void showRemoveAllOfflineContentDialog() {
        new AlertDialog.Builder(OfflineSettingsActivity.this)
                .setTitle(R.string.pref_offline_remove_all_offline_content)
                .setMessage(R.string.pref_offline_remove_all_offline_content_description)
                .setPositiveButton(R.string.btn_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                                            /* no-op yet */
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(true)
                .create()
                .show();
    }
}
