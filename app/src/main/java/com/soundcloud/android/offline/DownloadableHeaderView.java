package com.soundcloud.android.offline;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.AnimUtils;

import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

public class DownloadableHeaderView {
    private final Resources resources;

    @InjectView(R.id.download_state) ImageView downloadState;
    @InjectView(R.id.header_text) TextView headerView;

    private String headerText;
    private boolean isDownloading;

    @Inject
    public DownloadableHeaderView(Resources resources) {
        this.resources = resources;
        this.isDownloading = false;
    }

    public void onViewCreated(View view) {
        ButterKnife.inject(this, view);
    }

    public void setHeaderText(String text) {
        this.headerText = text;
        if (!isDownloading) {
            headerView.setText(headerText);
        }
    }

    public void showNoOfflineState() {
        isDownloading = false;
        downloadState.clearAnimation();
        downloadState.setVisibility(View.GONE);
        downloadState.setImageDrawable(null);
        headerView.setText(headerText);
    }

    public void showDownloadingState() {
        isDownloading = true;

        downloadState.clearAnimation();
        downloadState.setVisibility(View.VISIBLE);
        downloadState.setImageResource(R.drawable.header_syncing);
        headerView.setText(resources.getString(R.string.offline_update_in_progress));
        AnimUtils.runSpinClockwiseAnimationOn(downloadState.getContext(), downloadState);
    }

    public void showDownloadedState() {
        isDownloading = false;

        downloadState.clearAnimation();
        downloadState.setVisibility(View.VISIBLE);
        downloadState.setImageResource(R.drawable.header_downloaded);
        headerView.setText(headerText);
    }

    public void showRequestedState() {
        isDownloading = false;

        downloadState.clearAnimation();
        downloadState.setVisibility(View.VISIBLE);
        downloadState.setImageResource(R.drawable.entity_downloadable);
        headerView.setText(headerText);
    }
}
