package com.soundcloud.android.likes;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.offline.DownloadState;
import com.soundcloud.android.offline.DownloadableHeaderView;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import javax.inject.Inject;

class TrackLikesHeaderView {

    private View headerView;
    private final DownloadableHeaderView downloadableHeaderView;

    @InjectView(R.id.shuffle_btn) Button shuffleButton;
    @InjectView(R.id.overflow_button) ImageView overflowMenuButton;

    private int trackCount;

    @Inject
    TrackLikesHeaderView(DownloadableHeaderView downloadableHeaderView) {
        this.downloadableHeaderView = downloadableHeaderView;
    }

    @VisibleForTesting
    View getHeaderView() {
        return headerView;
    }

    void onViewCreated(View view) {
        headerView = View.inflate(view.getContext(), R.layout.track_likes_header, null);
        downloadableHeaderView.onViewCreated(headerView);
        ButterKnife.inject(this, headerView);
    }

    void onDestroyView() {
        ButterKnife.reset(this);
        headerView = null;
    }

    public void attachToList(ListView listView) {
        listView.addHeaderView(headerView, null, false);
    }

    void setOnShuffleButtonClick(View.OnClickListener listener) {
        shuffleButton.setOnClickListener(listener);
    }

    void showOverflowMenuButton() {
        overflowMenuButton.setVisibility(View.VISIBLE);
    }

    void hideOverflowMenuButton() {
        overflowMenuButton.setVisibility(View.GONE);
    }

    void setOnOverflowMenuClick(View.OnClickListener listener) {
        overflowMenuButton.setOnClickListener(listener);
    }

    public void show(DownloadState state) {
        downloadableHeaderView.show(state);
        if (state == DownloadState.NO_OFFLINE || state == DownloadState.DOWNLOADED) {
            updateTrackCount(trackCount);
        }
    }

    void updateTrackCount(int trackCount) {
        this.trackCount = trackCount;
        downloadableHeaderView.setHeaderText(getHeaderText(trackCount));
        updateShuffleButton(trackCount);
    }

    void updateOverflowMenuButton(boolean showOverflowMenu) {
        overflowMenuButton.setVisibility(showOverflowMenu ? View.VISIBLE : View.GONE);
    }

    private String getHeaderText(int likedTracks) {
        if (likedTracks == 0) {
            return headerView.getContext().getString(R.string.number_of_liked_tracks_you_liked_zero);
        } else {
            return headerView.getContext().getResources()
                    .getQuantityString(R.plurals.number_of_liked_tracks_you_liked, likedTracks, likedTracks);
        }
    }

    private void updateShuffleButton(int likedTracks) {
        if (likedTracks <= 1) {
            shuffleButton.setVisibility(View.GONE);
            shuffleButton.setEnabled(false);
        } else {
            shuffleButton.setVisibility(View.VISIBLE);
            shuffleButton.setEnabled(true);
        }
    }

}
