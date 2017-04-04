package com.soundcloud.android.settings;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.offline.OfflineSettingsStorage;

import android.content.Context;
import android.content.res.Resources;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

public final class OfflineStoragePreference extends Preference {

    private static final double ONE_GIGABYTE = 1024 * 1024 * 1024;

    @BindView(R.id.offline_storage_usage_bars) UsageBarView usageBarView;
    @BindView(R.id.offline_storage_limit_seek_bar) SeekBar storageLimitSeekBar;
    @BindView(R.id.offline_storage_limit) TextView storageLimitTextView;
    @BindView(R.id.offline_storage_free) TextView storageFreeTextView;
    @BindView(R.id.offline_storage_legend_other) TextView storageOtherLabelTextView;
    @BindView(R.id.offline_storage_legend_used) TextView storageUsedLabelTextView;
    @BindView(R.id.offline_storage_legend_limit) TextView storageLimitLabelTextView;

    private OnStorageLimitChangedListener onStorageLimitChangeListener;
    private OfflineUsage offlineUsage;
    private final Resources resources;

    private final SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        private boolean showLimitToast = false;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                showLimitToast = !offlineUsage.setOfflineLimitPercentage(progress);
                updateView();
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (onStorageLimitChangeListener != null) {
                final long newValue = offlineUsage.isUnlimited() ?
                                      OfflineSettingsStorage.UNLIMITED :
                                      offlineUsage.getActualOfflineLimit();
                onStorageLimitChangeListener.onStorageLimitChanged(newValue, showLimitToast);
                showLimitToast = false;
            }
        }
    };

    public OfflineStoragePreference(Context context, AttributeSet attr) {
        super(context, attr);
        setPersistent(false);
        setLayoutResource(R.layout.offline_storage_limit);
        resources = context.getResources();
    }

    void setOnStorageLimitChangedListener(OnStorageLimitChangedListener onPreferenceChangeListener) {
        onStorageLimitChangeListener = onPreferenceChangeListener;
    }

    void setOfflineUsage(OfflineUsage usage) {
        this.offlineUsage = usage;
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        ButterKnife.bind(this, view);
        storageLimitSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        updateAndRefresh();
        return view;
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        onStorageLimitChangeListener = null;
    }

    void updateAndRefresh() {
        if (offlineUsage != null) {
            offlineUsage.update();
            updateView();
        }
    }

    private void updateView() {
        if (usageBarView != null) {
            updateUsageBarView();
            updateLabels();
        }
    }

    private void updateLabels() {
        storageLimitSeekBar.setProgress(offlineUsage.getOfflineLimitPercentage());
        storageLimitTextView.setText(formatLimitGigabytes());

        storageFreeTextView.setText(formatFreeGigabytes());
        storageOtherLabelTextView.setText(formatGigabytes(offlineUsage.getUsedOthers()));
        storageUsedLabelTextView.setText(formatGigabytes(offlineUsage.getOfflineUsed()));
        storageLimitLabelTextView.setText(formatGigabytes(offlineUsage.getUsableOfflineLimit()));
    }

    private String formatGigabytes(long bytes) {
        return String.format(resources.getString(R.string.pref_offline_storage_limit_gb), bytesToGB(bytes));
    }

    private String formatFreeGigabytes() {
        return offlineUsage.isOfflineContentAccessible() ? getFreeGigabytes() : resources.getString(R.string.sd_card_unavailable);
    }

    private String getFreeGigabytes() {
        return String.format(resources.getString(R.string.pref_offline_storage_free_gb),
                      bytesToGB(offlineUsage.getDeviceAvailable()),
                      bytesToGB(offlineUsage.getDeviceTotal()));
    }

    private String formatLimitGigabytes() {
        if (offlineUsage.isUnlimited()) {
            return resources.getString(R.string.unlimited);
        } else {
            return formatGigabytes(offlineUsage.getActualOfflineLimit());
        }
    }

    private double bytesToGB(long bytes) {
        return bytes / ONE_GIGABYTE;
    }

    private void updateUsageBarView() {
        usageBarView.reset()
                    .addBar(R.color.usage_bar_other, offlineUsage.getUsedOthers())
                    .addBar(R.color.usage_bar_used, offlineUsage.getOfflineUsed())
                    .addBar(R.color.usage_bar_limit, offlineUsage.getOfflineAvailable())
                    .addBar(R.color.usage_bar_free, offlineUsage.getUnused());
    }

    interface OnStorageLimitChangedListener {
        void onStorageLimitChanged(long newStorageLimit, boolean belowLimitWarning);
    }
}
