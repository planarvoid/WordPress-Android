package com.soundcloud.android.settings;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.offline.OfflineContentLocation;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.RadioGroup;

import javax.inject.Inject;

class ChangeStorageLocationPresenter extends DefaultActivityLightCycle<AppCompatActivity> {

    private static final double ONE_GIGABYTE = 1024 * 1024 * 1024;

    @BindView(R.id.storage_options) RadioGroup storageOptions;
    @BindView(R.id.internal_device_storage) SummaryRadioButton storageRadioButton;
    @BindView(R.id.sd_card) SummaryRadioButton sdCardRadioButton;

    private AppCompatActivity activity;
    private final OfflineSettingsStorage offlineSettingsStorage;

    private Unbinder unbinder;

    @Inject
    ChangeStorageLocationPresenter(OfflineSettingsStorage offlineSettingsStorage) {
        this.offlineSettingsStorage = offlineSettingsStorage;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.activity = activity;
        unbinder = ButterKnife.bind(this, activity);
        updateSDCardCheckedState();
        updateSummaries();
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        unbinder.unbind();
        this.activity = null;
    }

    private void updateSDCardCheckedState() {
        sdCardRadioButton.setChecked(OfflineContentLocation.SD_CARD == offlineSettingsStorage.getOfflineContentLocation());
    }

    private void updateSummaries() {
        storageRadioButton.setSummary(formatGigabytes(IOUtils.getExternalStorageFreeSpace(activity.getApplicationContext()), IOUtils.getExternalStorageTotalSpace(activity.getApplicationContext())));
        sdCardRadioButton.setSummary(formatGigabytes(IOUtils.getSDCardStorageFreeSpace(activity.getApplicationContext()), IOUtils.getSDCardStorageTotalSpace(activity.getApplicationContext())));
    }

    private String formatGigabytes(long free, long total) {
        return String.format(activity.getResources().getString(R.string.pref_offline_storage_free_gb),
                             bytesToGB(free),
                             bytesToGB(total));
    }

    private double bytesToGB(long bytes) {
        return bytes / ONE_GIGABYTE;
    }
}
