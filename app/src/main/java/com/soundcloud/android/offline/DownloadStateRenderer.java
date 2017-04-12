package com.soundcloud.android.offline;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;

public class DownloadStateRenderer {

    private final Resources resources;
    private final FeatureFlags featureFlags;

    private String headerText;

    @Inject
    public DownloadStateRenderer(Resources resources, FeatureFlags featureFlags) {
        this.resources = resources;
        this.featureFlags = featureFlags;
    }

    public void setHeaderText(String text, View view) {
        this.headerText = text;
        if (!getDownloadStateView(view).isDownloading()) {
            getHeaderTextView(view).setText(headerText);
        }
    }

    public void show(OfflineState offlineState, View view) {
        DownloadImageView downloadStateView = getDownloadStateView(view);
        if (featureFlags.isEnabled(Flag.NEW_OFFLINE_ICONS)) {
            downloadStateView.setVisibility(View.GONE);
        } else {
            downloadStateView.setState(offlineState, false);
        }
        updateHeaderText(offlineState, view);
    }

    private void updateHeaderText(OfflineState offlineState, View view) {
        TextView headerTextView = getHeaderTextView(view);
        if (offlineState == OfflineState.DOWNLOADING) {
            headerTextView.setText(resources.getString(R.string.offline_update_in_progress));
        } else {
            headerTextView.setText(headerText);
        }
    }

    private TextView getHeaderTextView(View view){
        return ButterKnife.findById(view, R.id.header_text);
    }
    private DownloadImageView getDownloadStateView(View view){
        return ButterKnife.findById(view, R.id.header_download_state);
    }
}
