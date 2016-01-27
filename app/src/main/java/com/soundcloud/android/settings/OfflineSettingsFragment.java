package com.soundcloud.android.settings;

import static android.preference.Preference.OnPreferenceChangeListener;
import static android.preference.Preference.OnPreferenceClickListener;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.android.settings.SettingKey.OFFLINE_COLLECTION;
import static com.soundcloud.android.settings.SettingKey.OFFLINE_REMOVE_ALL_OFFLINE_CONTENT;
import static com.soundcloud.android.settings.SettingKey.OFFLINE_STORAGE_LIMIT;
import static com.soundcloud.android.settings.SettingKey.SUBSCRIBE;
import static com.soundcloud.android.settings.SettingKey.WIFI_ONLY;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.dialog.ImageAlertDialog;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineContentService;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.TwoStatePreference;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class OfflineSettingsFragment extends PreferenceFragment implements OnPreferenceClickListener, OnPreferenceChangeListener {

    @Inject OfflineSettingsStorage offlineSettings;
    @Inject OfflineUsage offlineUsage;
    @Inject OfflineContentOperations offlineContentOperations;
    @Inject FeatureOperations featureOperations;
    @Inject EventBus eventBus;
    @Inject Navigator navigator;
    @Inject FeatureFlags featureFlags;

    private CompositeSubscription subscription;

    public static OfflineSettingsFragment create() {
        return new OfflineSettingsFragment();
    }

    public OfflineSettingsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        subscription = new CompositeSubscription();
        if (featureOperations.isOfflineContentEnabled()) {
            addPreferencesFromResource(R.xml.settings_offline);
            setupOffline();
        } else if (featureOperations.upsellHighTier()) {
            addPreferencesFromResource(R.xml.settings_subscribe);
            setupUpsell();
        } else {
            addPreferencesFromResource(R.xml.settings_offline_clear);
            setupClearContent();
        }
        return view;
    }

    private void setupUpsell() {
        findPreference(SUBSCRIBE).setOnPreferenceClickListener(this);
        setupClearContent();
    }

    private void setupOffline() {
        OfflineStoragePreference offlineStorage = (OfflineStoragePreference) findPreference(OFFLINE_STORAGE_LIMIT);
        offlineStorage.setOnPreferenceChangeListener(this);
        offlineStorage.setOfflineUsage(offlineUsage);

        TwoStatePreference offlineCollection = (TwoStatePreference) findPreference(OFFLINE_COLLECTION);
        if (featureFlags.isDisabled(Flag.OFFLINE_SYNC_COLLECTION)) {
            getPreferenceScreen().removePreference(offlineCollection);
        } else {
            offlineCollection.setChecked(offlineContentOperations.isOfflineCollectionEnabled());
            offlineCollection.setOnPreferenceChangeListener(this);
        }

        TwoStatePreference wifi = (TwoStatePreference) findPreference(WIFI_ONLY);
        wifi.setChecked(offlineSettings.isWifiOnlyEnabled());
        wifi.setOnPreferenceChangeListener(this);

        setupClearContent();

        subscription.add(eventBus.subscribe(EventQueue.OFFLINE_CONTENT_CHANGED, new CurrentDownloadSubscriber()));
    }

    private void setupClearContent() {
        findPreference(OFFLINE_REMOVE_ALL_OFFLINE_CONTENT).setOnPreferenceClickListener(this);
    }

    @Override
    public void onDestroyView() {
        subscription.unsubscribe();
        super.onDestroyView();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case OFFLINE_COLLECTION:
                if (((boolean) newValue)) {
                    fireAndForget(offlineContentOperations.enableOfflineCollection());
                } else {
                    confirmDisableOfflineCollection();
                }
                return true;
            case OFFLINE_STORAGE_LIMIT:
                onUpdateStorageLimit((long) newValue);
                return true;
            case WIFI_ONLY:
                offlineSettings.setWifiOnlyEnabled((boolean) newValue);
                if (!offlineSettings.isWifiOnlyEnabled()) {
                    OfflineContentService.start(getActivity());
                }
                return true;
            default:
                return false;
        }
    }

    private void confirmDisableOfflineCollection() {
        new ImageAlertDialog(getActivity())
                .setContent(R.drawable.dialog_download,
                        R.string.disable_offline_collection_title,
                        R.string.disable_offline_collection_body)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        offlineContentOperations.disableOfflineCollection();
                        removeAllOfflineContent();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setOfflineCollectionChecked(true);
                    }
                })
                .show();
    }

    private void setOfflineCollectionChecked(boolean checked) {
        TwoStatePreference offlineCollection = (TwoStatePreference) findPreference(OFFLINE_COLLECTION);
        offlineCollection.setChecked(checked);
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
                        removeAllOfflineContent();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void removeAllOfflineContent() {
        subscription.add(offlineContentOperations
                .clearOfflineContent()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ClearOfflineContentSubscriber()));
    }

    private final class ClearOfflineContentSubscriber extends DefaultSubscriber<Void> {
        @Override
        public void onNext(Void ignored) {
            if (offlineContentOperations.isOfflineCollectionEnabled()) {
                offlineContentOperations.disableOfflineCollection();
                setOfflineCollectionChecked(false);
            }
            refreshStoragePreference();
        }
    }

    private final class CurrentDownloadSubscriber extends DefaultSubscriber<OfflineContentChangedEvent> {
        @Override
        public void onNext(final OfflineContentChangedEvent event) {
            if (event.state == OfflineState.DOWNLOADED) {
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
