package com.soundcloud.android.settings;

import static android.preference.Preference.OnPreferenceChangeListener;
import static android.preference.Preference.OnPreferenceClickListener;
import static com.soundcloud.android.settings.SettingKey.OFFLINE_REMOVE_ALL_OFFLINE_CONTENT;
import static com.soundcloud.android.settings.SettingKey.OFFLINE_STORAGE_LIMIT;
import static com.soundcloud.android.settings.SettingKey.SUBSCRIBE;
import static com.soundcloud.android.settings.SettingKey.WIFI_ONLY;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineContentService;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.TwoStatePreference;
import android.support.v7.app.AlertDialog;

import javax.inject.Inject;
import java.util.List;

public class OfflineSettingsFragment extends PreferenceFragment implements OnPreferenceClickListener, OnPreferenceChangeListener {

    @Inject OfflineSettingsStorage offlineSettings;
    @Inject OfflineUsage offlineUsage;
    @Inject OfflineContentOperations offlineContentOperations;
    @Inject FeatureOperations featureOperations;
    @Inject EventBus eventBus;
    @Inject Navigator navigator;

    private CompositeSubscription subscription;

    public static OfflineSettingsFragment create() {
        return new OfflineSettingsFragment();
    }

    public OfflineSettingsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        subscription = new CompositeSubscription();
        if (featureOperations.isOfflineContentEnabled()) {
            addPreferencesFromResource(R.xml.settings_offline);
            setupOffline();
        } else if (featureOperations.upsellMidTier()) {
            addPreferencesFromResource(R.xml.settings_subscribe);
            setupUpsell();
        } else {
            addPreferencesFromResource(R.xml.settings_offline_clear);
            setupClearContent();
        }
    }

    private void setupUpsell() {
        findPreference(SUBSCRIBE).setOnPreferenceClickListener(this);
        setupClearContent();
    }

    private void setupOffline() {
        OfflineStoragePreference offlineStorage = (OfflineStoragePreference) findPreference(OFFLINE_STORAGE_LIMIT);
        offlineStorage.setOnPreferenceChangeListener(this);
        offlineStorage.setOfflineUsage(offlineUsage);

        TwoStatePreference wifi = (TwoStatePreference) findPreference(WIFI_ONLY);
        wifi.setChecked(offlineSettings.isWifiOnlyEnabled());
        wifi.setOnPreferenceChangeListener(this);
        setupClearContent();

        subscription.add(eventBus.subscribe(EventQueue.CURRENT_DOWNLOAD, new CurrentDownloadSubscriber()));
    }

    private void setupClearContent() {
        findPreference(OFFLINE_REMOVE_ALL_OFFLINE_CONTENT).setOnPreferenceClickListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        subscription.unsubscribe();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case OFFLINE_STORAGE_LIMIT:
                onUpdateStorageLimit((long) newValue);
                return true;
            case WIFI_ONLY:
                offlineSettings.setWifiOnlyEnabled((boolean) newValue);
                return true;
            default:
                return false;
        }
    }

    private void onUpdateStorageLimit(long limit) {
        if (limit == OfflineSettingsStorage.UNLIMITED) {
            offlineSettings.setStorageUnlimited();
        } else {
            offlineSettings.setStorageLimit(limit);
        }
        OfflineContentService.start(getActivity());
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case SUBSCRIBE:
                openSubscribeScreen();
                return true;
            case OFFLINE_REMOVE_ALL_OFFLINE_CONTENT:
                showRemoveAllOfflineContentDialog();
                return true;
            default:
                return false;
        }
    }

    private void openSubscribeScreen() {
        navigator.openUpgrade(getActivity());
    }

    private void showRemoveAllOfflineContentDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.pref_offline_remove_all_offline_content)
                .setMessage(R.string.pref_offline_remove_all_offline_content_description)
                .setPositiveButton(R.string.btn_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        subscription.add(offlineContentOperations
                                .clearOfflineContent()
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new ClearOfflineContentSubscriber()));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private final class ClearOfflineContentSubscriber extends DefaultSubscriber<List<Urn>> {
        @Override
        public void onNext(List<Urn> result) {
            if (!result.isEmpty()) {
                refreshStoragePreference();
            }
            OfflineContentService.stop(getActivity());
        }
    }

    private final class CurrentDownloadSubscriber extends DefaultSubscriber<CurrentDownloadEvent> {
        @Override
        public void onNext(final CurrentDownloadEvent event) {
            if (event.kind == OfflineState.DOWNLOADED) {
                refreshStoragePreference();
            }
        }
    }

    private void refreshStoragePreference() {
        OfflineStoragePreference offlinePreferences = (OfflineStoragePreference) findPreference(OFFLINE_STORAGE_LIMIT);
        if (offlinePreferences != null) {
            offlinePreferences.updateAndRefresh();
        }
    }

}
