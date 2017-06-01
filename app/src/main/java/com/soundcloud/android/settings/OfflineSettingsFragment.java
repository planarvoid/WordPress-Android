package com.soundcloud.android.settings;

import static android.preference.Preference.OnPreferenceChangeListener;
import static android.preference.Preference.OnPreferenceClickListener;
import static com.soundcloud.android.offline.OfflineContentLocation.SD_CARD;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.android.settings.SettingKey.OFFLINE_CHANGE_STORAGE_LOCATION;
import static com.soundcloud.android.settings.SettingKey.OFFLINE_COLLECTION;
import static com.soundcloud.android.settings.SettingKey.OFFLINE_REMOVE_ALL_OFFLINE_CONTENT;
import static com.soundcloud.android.settings.SettingKey.OFFLINE_STORAGE_LIMIT;
import static com.soundcloud.android.settings.SettingKey.WIFI_ONLY;

import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.configuration.ConfigurationManager;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.dialog.CustomFontViewBuilder;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.offline.OfflineContentLocation;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineContentService;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.LeakCanaryWrapper;
import com.soundcloud.rx.eventbus.EventBus;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.TwoStatePreference;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Toast;

import javax.inject.Inject;

public class OfflineSettingsFragment extends PreferenceFragment
        implements OnPreferenceClickListener, OnPreferenceChangeListener, OfflineStoragePreference.OnStorageLimitChangedListener {

    @Inject OfflineSettingsStorage offlineSettings;
    @Inject OfflineUsage offlineUsage;
    @Inject OfflineContentOperations offlineContentOperations;
    @Inject FeatureOperations featureOperations;
    @Inject EventBus eventBus;
    @Inject OfflinePropertiesProvider offlinePropertiesProvider;
    @Inject NavigationExecutor navigationExecutor;
    @Inject FeatureFlags featureFlags;
    @Inject ConfigurationManager configurationManager;
    @Inject ApplicationProperties applicationProperties;
    @Inject LeakCanaryWrapper leakCanaryWrapper;
    @Inject ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    private CompositeSubscription subscription;

    public static OfflineSettingsFragment create() {
        return new OfflineSettingsFragment();
    }

    public OfflineSettingsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        subscription = new CompositeSubscription();
        addPreferencesFromResource(R.xml.settings_offline_listening);
        setupOffline();
    }

    private void setupOffline() {
        OfflineStoragePreference offlineStorage = (OfflineStoragePreference) findPreference(OFFLINE_STORAGE_LIMIT);
        offlineStorage.setOnStorageLimitChangedListener(this);
        offlineStorage.setOfflineUsage(offlineUsage);

        TwoStatePreference offlineCollection = (TwoStatePreference) findPreference(OFFLINE_COLLECTION);
        offlineCollection.setSummaryOff(changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.PREF_OFFLINE_OFFLINE_COLLECTION_DESCRIPTION_OFF));
        offlineCollection.setChecked(offlineContentOperations.isOfflineCollectionEnabled());
        offlineCollection.setOnPreferenceChangeListener(this);

        TwoStatePreference wifi = (TwoStatePreference) findPreference(WIFI_ONLY);
        wifi.setChecked(offlineSettings.isWifiOnlyEnabled());
        wifi.setOnPreferenceChangeListener(this);

        setupChangeStorageLocation();
        setupClearContent();
        setupSubscription();
    }

    private void setupChangeStorageLocation() {
        Preference preference = findPreference(OFFLINE_CHANGE_STORAGE_LOCATION);
        if (applicationProperties.canChangeOfflineContentLocation() && shouldShowChangeStorageLocationPreference()) {
            preference.setSummary(getChangeStorageLocationSummary());
            preference.setOnPreferenceClickListener(this);
        } else {
            getPreferenceScreen().removePreference(preference);
        }
    }

    private boolean shouldShowChangeStorageLocationPreference() {
        return IOUtils.isSDCardMounted(getActivity()) || SD_CARD == offlineSettings.getOfflineContentLocation();
    }

    private int getChangeStorageLocationSummary() {
        return OfflineContentLocation.DEVICE_STORAGE == offlineSettings.getOfflineContentLocation()
                              ? R.string.pref_offline_change_storage_location_description_device_storage
                              : R.string.pref_offline_change_storage_location_description_sd_card;
    }

    private void setupClearContent() {
        findPreference(OFFLINE_REMOVE_ALL_OFFLINE_CONTENT).setOnPreferenceClickListener(this);
    }

    private void setupSubscription() {
        if (featureFlags.isEnabled(Flag.OFFLINE_PROPERTIES_PROVIDER)) {
            subscription.add(offlinePropertiesProvider.states()
                                                      .observeOn(AndroidSchedulers.mainThread())
                                                      .subscribe(new CurrentDownloadSubscriber()));
        } else {
            subscription.add(eventBus.queue(EventQueue.OFFLINE_CONTENT_CHANGED)
                                     .filter(event -> event.state == OfflineState.DOWNLOADED)
                                     .observeOn(AndroidSchedulers.mainThread())
                                     .subscribe(new CurrentDownloadSubscriber()));
        }

        subscription.add(offlineSettings.getOfflineContentLocationChange()
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new ChangeStorageLocationSubscriber()));
    }

    @Override
    public void onDestroyView() {
        subscription.unsubscribe();
        super.onDestroyView();
        leakCanaryWrapper.watch(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case OFFLINE_COLLECTION:
                onAutomaticCollectionSyncToggle((boolean) newValue);
                return true;
            case WIFI_ONLY:
                onWifiOnlySyncToggle((boolean) newValue);
                return true;
            default:
                return false;
        }
    }

    private void onAutomaticCollectionSyncToggle(boolean automaticSyncEnabled) {
        if (automaticSyncEnabled) {
            fireAndForget(offlineContentOperations.enableOfflineCollection());
            eventBus.publish(EventQueue.TRACKING,
                             OfflineInteractionEvent.fromEnableCollectionSync(Screen.SETTINGS_OFFLINE.get()));
        } else {
            confirmDisableOfflineCollection();
        }
    }

    private void onWifiOnlySyncToggle(boolean wifiOnlyToggle) {
        offlineSettings.setWifiOnlyEnabled(wifiOnlyToggle);
        eventBus.publish(EventQueue.TRACKING, OfflineInteractionEvent.forOnlyWifiOverWifiToggle(wifiOnlyToggle));

        if (!offlineSettings.isWifiOnlyEnabled()) {
            OfflineContentService.start(getActivity());
        }
    }

    private void confirmDisableOfflineCollection() {
        final View view = new CustomFontViewBuilder(getActivity())
                .setContent(R.drawable.dialog_download,
                            R.string.disable_offline_collection_title,
                            changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.DISABLE_OFFLINE_COLLECTION_BODY))
                .get();

        new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> resetOfflineFeature())
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> setOfflineCollectionChecked(true))
                .show();
    }

    private void setOfflineCollectionChecked(boolean checked) {
        TwoStatePreference offlineCollection = (TwoStatePreference) findPreference(OFFLINE_COLLECTION);
        offlineCollection.setChecked(checked);
    }

    @Override
    public void onStorageLimitChanged(long limit, boolean belowLimitWarning) {
        showUsageBelowLimitWarning(belowLimitWarning);

        if (limit == OfflineSettingsStorage.UNLIMITED) {
            offlineSettings.setStorageUnlimited();
        } else {
            offlineSettings.setStorageLimit(limit);
        }
        OfflineContentService.start(getActivity());
    }

    private void showUsageBelowLimitWarning(boolean belowLimitWarning) {
        if (belowLimitWarning) {
            Toast.makeText(getActivity(),
                           R.string.offline_cannot_set_limit_below_usage, Toast.LENGTH_SHORT).show();
            eventBus.publish(EventQueue.TRACKING, OfflineInteractionEvent.forStorageBelowLimitImpression());
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case OFFLINE_REMOVE_ALL_OFFLINE_CONTENT:
                showRemoveAllOfflineContentDialog();
                return true;
            case OFFLINE_CHANGE_STORAGE_LOCATION:
                startActivity(new Intent(getActivity(), ChangeStorageLocationActivity.class));
                return true;
            default:
                return false;
        }
    }

    private void showRemoveAllOfflineContentDialog() {
        final View view = new CustomFontViewBuilder(getActivity())
                .setTitle(R.string.remove_offline_content_title)
                .setMessage(getRemoveAllOfflineContentDialogBody()).get();

        new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.btn_continue, (dialog, which) -> resetOfflineFeature())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private int getRemoveAllOfflineContentDialogBody() {
        return offlineContentOperations.isOfflineCollectionEnabled()
               ? changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.REMOVE_OFFLINE_CONTENT_BODY_SYNC_COLLECTION)
               : R.string.remove_offline_content_body_default;
    }

    private void resetOfflineFeature() {
        eventBus.publish(EventQueue.TRACKING,
                         OfflineInteractionEvent.fromDisableCollectionSync(Screen.SETTINGS_OFFLINE.get()));

        subscription.add(offlineContentOperations
                                 .disableOfflineFeature()
                                 .observeOn(AndroidSchedulers.mainThread())
                                 .subscribe(new ClearOfflineContentSubscriber()));
    }

    private final class ClearOfflineContentSubscriber extends DefaultSubscriber<Void> {
        @Override
        public void onNext(Void ignored) {
            setOfflineCollectionChecked(false);
            refreshStoragePreference();
        }
    }

    private final class CurrentDownloadSubscriber extends DefaultSubscriber<Object> {
        @Override
        public void onNext(final Object signal) {
            refreshStoragePreference();
        }
    }

    private final class ChangeStorageLocationSubscriber extends DefaultSubscriber<Void> {
        @Override
        public void onNext(Void ignored) {
            setupChangeStorageLocation();
            refreshStoragePreference();
        }
    }

    private void refreshStoragePreference() {
        OfflineStoragePreference offlinePreferences = (OfflineStoragePreference) findPreference(OFFLINE_STORAGE_LIMIT);
        if (offlinePreferences != null) {
            offlinePreferences.updateAndRefresh();
        }
    }

}
