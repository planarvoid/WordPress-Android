package com.soundcloud.android.offline;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;

import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;

public class DownloadStateView {

    private final Resources resources;

    @BindView(R.id.header_download_state) DownloadImageView downloadStateView;
    @BindView(R.id.header_text) TextView headerView;

    private String headerText;

    @Inject
    public DownloadStateView(Resources resources) {
        this.resources = resources;
    }

    public void onViewCreated(View view) {
        ButterKnife.bind(this, view);
    }

    public void setHeaderText(String text) {
        this.headerText = text;
        if (!downloadStateView.isDownloading()) {
            headerView.setText(headerText);
        }
    }

    public void show(OfflineState offlineState) {
        downloadStateView.setState(offlineState);
        updateHeaderText(offlineState);
    }

    private void updateHeaderText(OfflineState offlineState) {
        if (offlineState == OfflineState.DOWNLOADING) {
            headerView.setText(resources.getString(R.string.offline_update_in_progress));
        } else {
            headerView.setText(headerText);
        }
    }
}
