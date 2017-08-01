package com.soundcloud.android.settings;

import static com.soundcloud.android.offline.OfflineContentLocation.DEVICE_STORAGE;
import static com.soundcloud.android.offline.OfflineContentLocation.SD_CARD;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.dialog.CustomFontViewBuilder;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.offline.OfflineContentLocation;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RadioGroup;

import javax.inject.Inject;

class ChangeStorageLocationPresenter extends DefaultActivityLightCycle<AppCompatActivity> {

    private static final double ONE_GIGABYTE = 1024 * 1024 * 1024;

    @BindView(R.id.storage_options) RadioGroup storageOptionsRadioGroup;
    @BindView(R.id.internal_device_storage) SummaryRadioButton storageRadioButton;
    @BindView(R.id.sd_card) SummaryRadioButton sdCardRadioButton;

    private final OfflineSettingsStorage offlineSettingsStorage;
    private final OfflineContentOperations offlineContentOperations;
    private final EventBus eventBus;

    private AppCompatActivity activity;
    private Unbinder unbinder;

    @Inject
    ChangeStorageLocationPresenter(OfflineSettingsStorage offlineSettingsStorage, OfflineContentOperations offlineContentOperations, EventBus eventBus) {
        this.offlineSettingsStorage = offlineSettingsStorage;
        this.offlineContentOperations = offlineContentOperations;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.activity = activity;
        unbinder = ButterKnife.bind(this, activity);
        updateSummaries();
        updateRadioGroup();
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        unbinder.unbind();
        this.activity = null;
    }

    private void updateSummaries() {
        storageRadioButton.setSummary(formatGigabytes(IOUtils.getExternalStorageFreeSpace(activity.getApplicationContext()), IOUtils.getExternalStorageTotalSpace(activity.getApplicationContext())));
        sdCardRadioButton.setSummary(offlineSettingsStorage.isOfflineContentAccessible()
                                     ? formatGigabytes(IOUtils.getSDCardStorageFreeSpace(activity.getApplicationContext()), IOUtils.getSDCardStorageTotalSpace(activity.getApplicationContext()))
                                     : activity.getString(R.string.unavailable));
    }

    private String formatGigabytes(long free, long total) {
        return String.format(activity.getResources().getString(R.string.pref_offline_storage_free_gb),
                             bytesToGB(free),
                             bytesToGB(total));
    }

    private double bytesToGB(long bytes) {
        return bytes / ONE_GIGABYTE;
    }

    private void updateRadioGroup() {
        storageOptionsRadioGroup.check(DEVICE_STORAGE == offlineSettingsStorage.getOfflineContentLocation()
                                       ? R.id.internal_device_storage
                                       : R.id.sd_card);
        storageOptionsRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch(checkedId) {
                case R.id.internal_device_storage:
                    showDialog(DEVICE_STORAGE);
                    break;
                case R.id.sd_card:
                    showDialog(SD_CARD);
                    break;
                default:
                    break;
            }
        });
    }

    private void showDialog(OfflineContentLocation offlineContentLocation) {
        final View view = new CustomFontViewBuilder(activity)
                .setTitle(R.string.confirm_change_storage_location_dialog_title)
                .setMessage(DEVICE_STORAGE == offlineContentLocation
                            ? R.string.confirm_change_storage_location_dialog_message_internal_device_storage
                            : R.string.confirm_change_storage_location_dialog_message_sd_card)
                .get();

        new AlertDialog.Builder(activity)
                .setView(view)
                .setPositiveButton(R.string.ok_got_it, (dialog, which) -> resetOfflineContent(offlineContentLocation))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> handleCancel())
                .setOnCancelListener((dialog) -> handleCancel())
                .create()
                .show();

        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SETTINGS_OFFLINE_STORAGE_LOCATION_CONFIRM));
    }

    private void resetOfflineContent(OfflineContentLocation offlineContentLocation) {
        eventBus.publish(EventQueue.TRACKING, OfflineContentLocation.DEVICE_STORAGE == offlineContentLocation
                                              ? OfflineInteractionEvent.forOfflineStorageLocationDevice()
                                              : OfflineInteractionEvent.forOfflineStorageLocationSdCard());
        offlineContentOperations.resetOfflineContent(offlineContentLocation).subscribe(new DefaultSingleObserver<>());
        activity.finish();
    }

    private void handleCancel() {
        storageOptionsRadioGroup.setOnCheckedChangeListener(null);
        updateRadioGroup();
    }

}
