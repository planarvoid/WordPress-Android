package com.soundcloud.android.settings;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.Resources;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public final class OfflineStoragePreference extends Preference {

    public static final long UNLIMITED = -1;

    private static final double ONE_GIGABYTE = 1024 * 1024 * 1024;

    @InjectView(R.id.offline_storage_usage_bars) UsageBarView usageBarView;
    @InjectView(R.id.offline_storage_limit_seek_bar) SeekBar storageLimitSeekBar;
    @InjectView(R.id.offline_storage_limit) TextView storageLimitTextView;
    @InjectView(R.id.offline_storage_free) TextView storageFreeTextView;
    @InjectView(R.id.offline_storage_legend_other) TextView storageOtherLabelTextView;
    @InjectView(R.id.offline_storage_legend_used) TextView storageUsedLabelTextView;
    @InjectView(R.id.offline_storage_legend_limit) TextView storageLimitLabelTextView;

    private OnPreferenceChangeListener onStorageLimitChangeListener;
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

                onStorageLimitChangeListener.onPreferenceChange(OfflineStoragePreference.this,
                        offlineUsage.isUnlimited() ? UNLIMITED : offlineUsage.getActualOfflineLimit());

                if (showLimitToast) {
                    Toast.makeText(getContext(),
                            R.string.offline_cannot_set_limit_below_usage, Toast.LENGTH_SHORT).show();
                    showLimitToast = false;
                }
            }
        }
    };

    public OfflineStoragePreference(Context context, AttributeSet attr) {
        super(context, attr);
        setPersistent(false);
        setLayoutResource(R.layout.offline_storage_limit);
        resources = context.getResources();
    }

    @Override
    public void setOnPreferenceChangeListener(OnPreferenceChangeListener onPreferenceChangeListener) {
        onStorageLimitChangeListener = onPreferenceChangeListener;
    }

    public void setOfflineUsage(OfflineUsage usage) {
        this.offlineUsage = usage;
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        ButterKnife.inject(this, view);
        storageLimitSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        updateAndRefresh();
        return view;
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        onStorageLimitChangeListener = null;
    }

    public void updateAndRefresh() {
        if (offlineUsage != null) {
            offlineUsage.update();
        }
        updateView();
    }

    private void updateView() {
        updateUsageBarView();
        updateLabels();
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

}
