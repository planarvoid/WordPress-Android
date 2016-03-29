package com.soundcloud.android.likes;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.offline.DownloadStateView;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.view.IconToggleButton;
import com.soundcloud.java.optional.Optional;

import android.content.res.Resources;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.widget.Checkable;
import android.widget.ImageButton;

@AutoFactory(allowSubclasses = true)
class TrackLikesHeaderView {

    private Resources resources;
    private DownloadStateView downloadStateView;

    @Bind(R.id.shuffle_btn) ImageButton shuffleButton;
    @Bind(R.id.toggle_download) IconToggleButton downloadToggle;

    private Optional<View> headerOpt;

    private int trackCount;
    private final Listener listener;

    interface Listener {
        void onShuffle();
        void onUpsell();
        void onMakeAvailableOffline(boolean isAvailable);
    }

    TrackLikesHeaderView(@Provided Resources resources,
                         @Provided DownloadStateView downloadStateView,
                         View view,
                         Listener listener) {
        this.resources = resources;
        this.downloadStateView = downloadStateView;
        this.listener = listener;

        setupView(view, downloadStateView, listener);
    }

    private void setupView(View view, DownloadStateView downloadStateView, final TrackLikesHeaderView.Listener listener) {
        final View headerView = view.findViewById(R.id.track_likes_header);
        downloadStateView.onViewCreated(headerView);
        headerOpt = Optional.of(headerView);

        ButterKnife.bind(this, headerView);
        shuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onShuffle();
            }
        });
    }

    @VisibleForTesting
    View getHeaderView() {
        return headerOpt.get();
    }

    public void show(OfflineState state) {
        downloadStateView.show(state);
        if (state == OfflineState.NOT_OFFLINE || state == OfflineState.DOWNLOADED) {
            updateTrackCount(trackCount);
        }
    }

    public void showNoWifi() {
        downloadStateView.setHeaderText(resources.getString(R.string.offline_no_wifi));
    }

    public void showNoConnection() {
        downloadStateView.setHeaderText(resources.getString(R.string.offline_no_connection));
    }

    public void setDownloadedButtonState(final boolean isOffline) {
        downloadToggle.setVisibility(View.VISIBLE);
        downloadToggle.setChecked(isOffline);
        downloadToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean changedState = ((Checkable) v).isChecked();
                downloadToggle.setChecked(!changedState); // Ignore isChecked - button is subscribed to state changes
                listener.onMakeAvailableOffline(!isOffline);
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
        if (headerOpt.isPresent()) {
            headerOpt.get().setVisibility(trackCount == 0 ? View.GONE : View.VISIBLE);
            downloadStateView.setHeaderText(getLikedTrackText(trackCount));
            updateShuffleButton(trackCount);
        }
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
