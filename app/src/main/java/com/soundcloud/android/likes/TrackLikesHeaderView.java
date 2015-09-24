package com.soundcloud.android.likes;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.offline.DownloadableHeaderView;
import com.soundcloud.android.offline.OfflineState;

import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.widget.Button;

import javax.inject.Inject;

class TrackLikesHeaderView {

    @Nullable private View headerView;
    private final Resources resources;
    private final DownloadableHeaderView downloadableHeaderView;

    @Bind(R.id.shuffle_btn) Button shuffleButton;
    @Bind(R.id.overflow_button) View overflowMenuButton;

    private int trackCount;

    @Inject
    TrackLikesHeaderView(Resources resources, DownloadableHeaderView downloadableHeaderView) {
        this.resources = resources;
        this.downloadableHeaderView = downloadableHeaderView;
    }

    @VisibleForTesting
    View getHeaderView() {
        return headerView;
    }

    void onViewCreated(View view) {
        headerView = view.findViewById(R.id.track_likes_header);
        downloadableHeaderView.onViewCreated(headerView);
        ButterKnife.bind(this, headerView);
    }

    void onDestroyView() {
        downloadableHeaderView.onDestroyView();
        ButterKnife.unbind(this);
        headerView = null;
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

    public void show(OfflineState state) {
        downloadableHeaderView.show(state);
        if (state == OfflineState.NO_OFFLINE || state == OfflineState.DOWNLOADED) {
            updateTrackCount(trackCount);
        }
    }

    void updateTrackCount(int trackCount) {
        this.trackCount = trackCount;
        headerView.setVisibility(trackCount == 0 ? View.GONE : View.VISIBLE);
        downloadableHeaderView.setHeaderText(getHeaderText(trackCount));
        updateShuffleButton(trackCount);
    }

    void updateOverflowMenuButton(boolean showOverflowMenu) {
        overflowMenuButton.setVisibility(showOverflowMenu ? View.VISIBLE : View.GONE);
    }

    private String getHeaderText(int likedTracks) {
        return resources.getQuantityString(R.plurals.number_of_liked_tracks_you_liked, likedTracks, likedTracks);
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
