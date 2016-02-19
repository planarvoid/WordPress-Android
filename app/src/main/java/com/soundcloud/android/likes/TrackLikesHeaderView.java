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
import android.widget.ImageButton;
import android.widget.ToggleButton;

import javax.inject.Inject;

class TrackLikesHeaderView {

    @Nullable private View headerView;
    private final Resources resources;
    private final DownloadableHeaderView downloadableHeaderView;

    @Bind(R.id.shuffle_btn) ImageButton shuffleButton;
    @Bind(R.id.toggle_download) ToggleButton downloadToggle;

    private int trackCount;
    private LikesHeaderListener listener;

    interface LikesHeaderListener {
        void onShuffle();
        void onUpsell();
        void onMakeAvailableOffline(boolean isAvailable);
    }

    @Inject
    TrackLikesHeaderView(Resources resources, DownloadableHeaderView downloadableHeaderView) {
        this.resources = resources;
        this.downloadableHeaderView = downloadableHeaderView;
    }

    @VisibleForTesting
    View getHeaderView() {
        return headerView;
    }

    void onViewCreated(View view, final LikesHeaderListener listener) {
        this.listener = listener;
        headerView = view.findViewById(R.id.track_likes_header);
        downloadableHeaderView.onViewCreated(headerView);
        ButterKnife.bind(this, headerView);
        shuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onShuffle();
            }
        });
    }

    public boolean isViewCreated() {
        return headerView != null;
    }

    void onDestroyView() {
        downloadableHeaderView.onDestroyView();
        ButterKnife.unbind(this);
        headerView = null;
        listener = null;
    }

    public void show(OfflineState state) {
        downloadableHeaderView.show(state);
        if (state == OfflineState.NOT_OFFLINE || state == OfflineState.DOWNLOADED) {
            updateTrackCount(trackCount);
        }
    }

    public void setDownloadedButtonState(final boolean isOffline) {
        downloadToggle.setVisibility(View.VISIBLE);
        downloadToggle.setChecked(isOffline);
        downloadToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onMakeAvailableOffline(!isOffline);
                downloadToggle.setChecked(false);
            }
        });
    }

    public void showUpsell() {
        downloadToggle.setVisibility(View.VISIBLE);
        downloadToggle.setChecked(false);
        downloadToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onUpsell();
                downloadToggle.setChecked(false);
            }
        });
    }

    void updateTrackCount(int trackCount) {
        this.trackCount = trackCount;
        headerView.setVisibility(trackCount == 0 ? View.GONE : View.VISIBLE);
        downloadableHeaderView.setHeaderText(getHeaderText(trackCount));
        updateShuffleButton(trackCount);
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
