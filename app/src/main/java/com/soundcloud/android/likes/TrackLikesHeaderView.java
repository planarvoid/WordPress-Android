package com.soundcloud.android.likes;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.offline.DownloadStateRenderer;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.view.IconToggleButton;

import android.content.res.Resources;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.widget.Checkable;
import android.widget.ImageButton;

@AutoFactory(allowSubclasses = true)
class TrackLikesHeaderView {

    private Resources resources;
    private DownloadStateRenderer downloadStateRenderer;

    @BindView(R.id.shuffle_btn) ImageButton shuffleButton;
    @BindView(R.id.toggle_download) IconToggleButton downloadToggle;

    private final View headerView;

    private int trackCount = Consts.NOT_SET;
    private final Listener listener;

    interface Listener {
        void onShuffle();

        void onUpsell();

        void onMakeAvailableOffline(boolean isAvailable);
    }

    TrackLikesHeaderView(@Provided Resources resources,
                         @Provided DownloadStateRenderer downloadStateRenderer,
                         View view,
                         Listener listener) {
        this.resources = resources;
        this.downloadStateRenderer = downloadStateRenderer;
        this.listener = listener;
        this.headerView = view.findViewById(R.id.track_likes_header);

        ButterKnife.bind(this, headerView);
        shuffleButton.setOnClickListener(v -> listener.onShuffle());
        if (trackCount >= 0) {
            updateTrackCount(trackCount);
        }
    }

    @VisibleForTesting
    View getHeaderView() {
        return headerView;
    }

    public void show(OfflineState state) {
        downloadStateRenderer.show(state, headerView);
        if (state == OfflineState.NOT_OFFLINE || state == OfflineState.DOWNLOADED) {
            updateTrackCount(trackCount);
        }
    }

    void showNoWifi() {
        downloadStateRenderer.setHeaderText(resources.getString(R.string.offline_no_wifi), headerView);
    }

    void showNoConnection() {
        downloadStateRenderer.setHeaderText(resources.getString(R.string.offline_no_connection), headerView);
    }

    void setDownloadedButtonState(final boolean isOffline) {
        downloadToggle.setVisibility(View.VISIBLE);
        downloadToggle.setChecked(isOffline);
        downloadToggle.setOnClickListener(v -> {
            boolean changedState = ((Checkable) v).isChecked();
            downloadToggle.setChecked(!changedState); // Ignore isChecked - button is subscribed to state changes
            listener.onMakeAvailableOffline(!isOffline);
        });
    }

    void showUpsell() {
        downloadToggle.setVisibility(View.VISIBLE);
        downloadToggle.setChecked(false);
        downloadToggle.setOnClickListener(v -> {
            listener.onUpsell();
            downloadToggle.setChecked(false);
        });
    }

    void updateTrackCount(int trackCount) {
        this.trackCount = trackCount;
        headerView.setVisibility(trackCount == 0 ? View.GONE : View.VISIBLE);
        downloadStateRenderer.setHeaderText(getLikedTrackText(trackCount), headerView);
        updateShuffleButton(trackCount);
    }

    private String getLikedTrackText(int likedTracks) {
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
