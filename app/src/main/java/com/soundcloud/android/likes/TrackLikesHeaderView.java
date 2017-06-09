package com.soundcloud.android.likes;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.GoOnboardingTooltipExperiment;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.view.OfflineStateButton;

import android.content.res.Resources;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

@AutoFactory(allowSubclasses = true)
class TrackLikesHeaderView {

    private final Resources resources;
    private final GoOnboardingTooltipExperiment goOnboardingTooltipExperiment;
    private final IntroductoryOverlayPresenter introductoryOverlayPresenter;

    @BindView(R.id.shuffle_btn) ImageButton shuffleButton;
    @BindView(R.id.offline_state_button) OfflineStateButton offlineStateButton;
    @BindView(R.id.header_text) TextView headerText;

    private final View headerView;

    private int trackCount = Consts.NOT_SET;
    private final Listener listener;

    interface Listener {
        void onShuffle();

        void onUpsell();

        void onMakeAvailableOffline(boolean isAvailable);
    }

    TrackLikesHeaderView(@Provided Resources resources,
                         @Provided GoOnboardingTooltipExperiment goOnboardingTooltipExperiment,
                         @Provided IntroductoryOverlayPresenter introductoryOverlayPresenter,
                         View view,
                         Listener listener) {
        this.resources = resources;
        this.goOnboardingTooltipExperiment = goOnboardingTooltipExperiment;
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
        offlineStateButton.setState(state);
    }

    void showOfflineIntroductoryOverlay() {
        if (goOnboardingTooltipExperiment.isEnabled()) {
            introductoryOverlayPresenter.showIfNeeded(IntroductoryOverlayKey.LISTEN_OFFLINE_LIKES,
                                                      offlineStateButton,
                                                      R.string.overlay_listen_offline_likes_title,
                                                      R.string.overlay_listen_offline_likes_description);
        }
    }

    void showNoWifi() {
        offlineStateButton.showNoWiFi();
    }

    void showNoConnection() {
        offlineStateButton.showNoConnection();
    }

    void setDownloadedButtonState(final boolean isOffline) {
        offlineStateButton.setVisibility(View.VISIBLE);
        offlineStateButton.setOnClickListener(v -> listener.onMakeAvailableOffline(!isOffline));
    }

    void showUpsell() {
        offlineStateButton.setVisibility(View.VISIBLE);
        offlineStateButton.setOnClickListener(v -> listener.onUpsell());
    }

    void updateTrackCount(int trackCount) {
        this.trackCount = trackCount;
        headerView.setVisibility(trackCount == 0 ? View.GONE : View.VISIBLE);
        headerText.setText(resources.getQuantityString(R.plurals.number_of_liked_tracks_you_liked, trackCount, trackCount));
        updateShuffleButton(trackCount);
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
