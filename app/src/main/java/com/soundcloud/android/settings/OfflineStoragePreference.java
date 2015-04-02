package com.soundcloud.android.settings;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.Resources;
import android.preference.Preference;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

public final class OfflineStoragePreference extends Preference {
    private static final double ONE_GIGABYTE = 1024*1024*1024;

    private UsageBarView usageBarView;
    private SeekBar storageLimitSeekBar;
    private TextView storageLimitTextView;
    private TextView storageFreeTextView;
    private TextView storageOtherLabelTextView;
    private TextView storageUsedLabelTextView;
    private TextView storageLimitLabelTextView;
    private OnPreferenceChangeListener onStorageLimitChangeListener;
    private OfflineUsage offlineUsage;

    private final SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser) {
                offlineUsage.setOfflineTotalPercentage(progress);
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
                        offlineUsage.getOfflineTotal());
            }
        }
    };

    public OfflineStoragePreference(Context context, AttributeSet attr) {
        super(context, attr);
        setPersistent(false);
        setLayoutResource(R.layout.offline_storage_limit);
    }

    public void setOnStorageLimitChangeListener(OnPreferenceChangeListener onPreferenceChangeListener) {
        onStorageLimitChangeListener = onPreferenceChangeListener;
    }

    public void setOfflineUsage(OfflineUsage usage) {
        this.offlineUsage = usage;
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);

        storageLimitSeekBar = (SeekBar) view.findViewById(R.id.offline_storage_limit_seek_bar);
        storageLimitSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);

        usageBarView = (UsageBarView) view.findViewById(R.id.offline_storage_usage_bars);
        storageLimitTextView = (TextView) view.findViewById(R.id.offline_storage_limit);
        storageFreeTextView = (TextView) view.findViewById(R.id.offline_storage_free);
        storageOtherLabelTextView = (TextView) view.findViewById(R.id.offline_storage_legend_other);
        storageUsedLabelTextView = (TextView) view.findViewById(R.id.offline_storage_legend_used);
        storageLimitLabelTextView = (TextView) view.findViewById(R.id.offline_storage_legend_limit);

        updateAndRefresh();
        return view;
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        onStorageLimitChangeListener = null;
    }

    public void updateAndRefresh() {
        if(offlineUsage != null) {
            offlineUsage.update();
        }
        updateView();
    }

    private void updateView() {
        updateUsageBarView();
        updateLabels();
    }

    private void updateLabels() {
        storageLimitSeekBar.setProgress(offlineUsage.getOfflineTotalPercentage());
        storageFreeTextView.setText(formatFreeGigabytes());
        storageLimitTextView.setText(formatGigabytes(offlineUsage.getOfflineTotal()));
        storageOtherLabelTextView.setText(formatGigabytes(offlineUsage.getUsedOthers()));
        storageUsedLabelTextView.setText(formatGigabytes(offlineUsage.getOfflineUsed()));
        storageLimitLabelTextView.setText(formatGigabytes(offlineUsage.getOfflineTotal()));
    }

    private String formatGigabytes(long bytes) {
        Resources resources = getContext().getResources();
        return String.format(resources.getString(R.string.pref_offline_storage_limit_gb), bytesToGB(bytes));
    }

    private String formatFreeGigabytes() {
        Resources resources = getContext().getResources();
        return String.format(resources.getString(R.string.pref_offline_storage_free_gb),
                bytesToGB(offlineUsage.getDeviceAvailable()),
                bytesToGB(offlineUsage.getDeviceTotal()));
    }

    private double bytesToGB(long bytes) {
        return bytes / ONE_GIGABYTE;
    }

    private void updateUsageBarView() {
        usageBarView.reset()
                .addBar(R.color.usage_bar_other, offlineUsage.getUsedOthers())
                .addBar(R.color.usage_bar_used, offlineUsage.getOfflineUsed())
                .addBar(R.color.usage_bar_limit, offlineUsage.getOfflineAvailable())
                .addBar(R.color.usage_bar_free, offlineUsage.getAvailableWithoutOfflineLimit());
    }
}
