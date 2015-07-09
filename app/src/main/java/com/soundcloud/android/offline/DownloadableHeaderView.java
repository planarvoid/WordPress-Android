package com.soundcloud.android.offline;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.soundcloud.android.R;

import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;

public class DownloadableHeaderView {
    private final Resources resources;

    @InjectView(R.id.header_download_state) DownloadImageView downloadStateView;
    @InjectView(R.id.header_text) TextView headerView;

    private String headerText;

    @Inject
    public DownloadableHeaderView(Resources resources) {
        this.resources = resources;
    }

    public void onViewCreated(View view) {
        ButterKnife.inject(this, view);
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
