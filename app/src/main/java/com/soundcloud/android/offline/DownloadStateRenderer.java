package com.soundcloud.android.offline;

import butterknife.ButterKnife;
import com.soundcloud.android.R;

import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;

public class DownloadStateRenderer {

    private final Resources resources;
    private String headerText;

    @Inject
    public DownloadStateRenderer(Resources resources) {
        this.resources = resources;
    }

    public void setHeaderText(String text, View view) {
        this.headerText = text;
        if (!getDownloadStateView(view).isDownloading()) {
            getHeaderTextView(view).setText(headerText);
        }
    }

    public void show(OfflineState offlineState, View view) {
        getDownloadStateView(view).setState(offlineState);
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
