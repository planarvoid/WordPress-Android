package com.soundcloud.android.likes;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter;
import com.soundcloud.android.offline.DownloadStateRenderer;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.view.IconToggleButton;
import com.soundcloud.android.view.OfflineStateButton;

import android.content.res.Resources;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.widget.Checkable;
import android.widget.ImageButton;

@AutoFactory(allowSubclasses = true)
class TrackLikesHeaderView {

    private final Resources resources;
    private final DownloadStateRenderer downloadStateRenderer;
    private final FeatureFlags featureFlags;
    private final IntroductoryOverlayPresenter introductoryOverlayPresenter;

    @BindView(R.id.shuffle_btn) ImageButton shuffleButton;
    @BindView(R.id.toggle_download) IconToggleButton downloadToggle;
    @BindView(R.id.offline_state_button) OfflineStateButton offlineStateButton;

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
                         @Provided FeatureFlags featureFlags,
                         @Provided IntroductoryOverlayPresenter introductoryOverlayPresenter,
                         View view,
                         Listener listener) {
        this.resources = resources;
        this.downloadStateRenderer = downloadStateRenderer;
        this.featureFlags = featureFlags;
        this.introductoryOverlayPresenter = introductoryOverlayPresenter;
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
        if (featureFlags.isEnabled(Flag.NEW_OFFLINE_ICONS)) {
            offlineStateButton.setState(state);
        } else {
            downloadStateRenderer.show(state, headerView);
            if (state == OfflineState.NOT_OFFLINE || state == OfflineState.DOWNLOADED) {
                updateTrackCount(trackCount);
            }
        }
    }

    void showOfflineIntroductoryOverlay() {
        if (featureFlags.isEnabled(Flag.NEW_OFFLINE_ICONS) && featureFlags.isEnabled(Flag.COLLECTION_OFFLINE_ONBOARDING)) {
            introductoryOverlayPresenter.showIfNeeded(IntroductoryOverlayKey.LISTEN_OFFLINE_LIKES,
                                                      offlineStateButton,
                                                      R.string.overlay_listen_offline_likes_title,
                                                      R.string.overlay_listen_offline_likes_description);
        }
    }

    void showNoWifi() {
        if (featureFlags.isEnabled(Flag.NEW_OFFLINE_ICONS)) {
            offlineStateButton.showNoWiFi();
        } else {
            downloadStateRenderer.setHeaderText(resources.getString(R.string.offline_no_wifi), headerView);
        }
    }

    void showNoConnection() {
        if (featureFlags.isEnabled(Flag.NEW_OFFLINE_ICONS)) {
            offlineStateButton.showNoConnection();
        } else {
            downloadStateRenderer.setHeaderText(resources.getString(R.string.offline_no_connection), headerView);
        }
    }

    void setDownloadedButtonState(final boolean isOffline) {
        if (featureFlags.isEnabled(Flag.NEW_OFFLINE_ICONS)) {
            offlineStateButton.setVisibility(View.VISIBLE);
            offlineStateButton.setOnClickListener(v -> listener.onMakeAvailableOffline(!isOffline));
        } else {
            showLegacyDownloadToggle(isOffline);
        }
    }

    private void showLegacyDownloadToggle(boolean isOffline) {
        downloadToggle.setVisibility(View.VISIBLE);
        downloadToggle.setChecked(isOffline);
        downloadToggle.setOnClickListener(v -> {
            boolean changedState = ((Checkable) v).isChecked();
            downloadToggle.setChecked(!changedState); // Ignore isChecked - button is subscribed to state changes
            listener.onMakeAvailableOffline(!isOffline);
        });
    }

    void showUpsell() {
        if (featureFlags.isEnabled(Flag.NEW_OFFLINE_ICONS)) {
            offlineStateButton.setVisibility(View.VISIBLE);
            offlineStateButton.setOnClickListener(v -> listener.onUpsell());
        } else {
            showLegacyUpsell();
        }
    }

    private void showLegacyUpsell() {
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
