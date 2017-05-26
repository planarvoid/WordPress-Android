package com.soundcloud.android.collection;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.java.optional.Optional;

import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.Arrays;

public class PlaylistItemIndicatorsView {

    @BindView(R.id.private_indicator) View privateIndicator;
    @BindView(R.id.item_download_state) DownloadImageView oldOfflineIndicator;
    @BindView(R.id.offline_state_indicator) DownloadImageView offlineIndicator;
    @BindView(R.id.no_network_indicator) ImageView noNetworkIndicator;
    @BindView(R.id.like_indicator) View likeIndicator;

    private final FeatureFlags featureFlags;
    private final OfflineSettingsOperations offlineSettingsOperations;
    private final ConnectionHelper connectionHelper;
    private final ChangeLikeToSaveExperiment changeLikeToSaveExperiment;

    @Inject
    PlaylistItemIndicatorsView(FeatureFlags featureFlags,
                               OfflineSettingsOperations offlineSettingsOperations,
                               ConnectionHelper connectionHelper,
                               ChangeLikeToSaveExperiment changeLikeToSaveExperiment) {
        this.featureFlags = featureFlags;
        this.offlineSettingsOperations = offlineSettingsOperations;
        this.connectionHelper = connectionHelper;
        this.changeLikeToSaveExperiment = changeLikeToSaveExperiment;
    }

    public void setupView(View view, boolean isPrivate, boolean isLiked, Optional<OfflineState> offlineState) {
        ButterKnife.bind(this, view);
        privateIndicator.setVisibility(isPrivate ? View.VISIBLE : View.GONE);
        setupOfflineAndLikeIndicators(isLiked, offlineState);
    }

    private void setupOfflineAndLikeIndicators(boolean isLiked, Optional<OfflineState> offlineState) {
        ViewUtils.setGone(Arrays.asList(oldOfflineIndicator, offlineIndicator, noNetworkIndicator, likeIndicator));
        if (featureFlags.isEnabled(Flag.NEW_OFFLINE_ICONS)) {
            showOfflineOrLikeIndicator(isLiked, offlineState);
        } else {
            setupOldOfflineAndLikeIndicators(isLiked, offlineState);
        }
    }

    private void showOfflineOrLikeIndicator(boolean isLiked, Optional<OfflineState> offlineState) {
        if (shouldShowOfflineIndicator(offlineState)) {
            showOfflineIndicator(offlineState.get());
        } else {
            offlineIndicator.setState(OfflineState.NOT_OFFLINE, true);
            likeIndicator.setVisibility(shouldShowLikeIndicator(isLiked)
                                        ? View.VISIBLE
                                        : View.GONE);
        }
    }

    private boolean shouldShowOfflineIndicator(Optional<OfflineState> offlineState) {
        return offlineState.isPresent() && OfflineState.NOT_OFFLINE != offlineState.get();
    }

    private void showOfflineIndicator(OfflineState offlineState) {
        if (OfflineState.REQUESTED == offlineState && shouldShowNoNetworkIndicator()) {
            offlineIndicator.setState(OfflineState.NOT_OFFLINE, true);
            noNetworkIndicator.setVisibility(View.VISIBLE);
        } else {
            offlineIndicator.setState(offlineState, true);
        }
    }

    private boolean shouldShowNoNetworkIndicator() {
        return (offlineSettingsOperations.isWifiOnlyEnabled() && !connectionHelper.isWifiConnected())
                || !connectionHelper.isNetworkConnected();
    }

    private void setupOldOfflineAndLikeIndicators(boolean isLiked, Optional<OfflineState> offlineState) {
        oldOfflineIndicator.setState(offlineState.or(OfflineState.NOT_OFFLINE), false);
        likeIndicator.setVisibility(shouldShowLikeIndicator(isLiked)
                                    ? View.VISIBLE
                                    : View.GONE);
    }

    private boolean shouldShowLikeIndicator(boolean isLiked) {
        return isLiked && !changeLikeToSaveExperiment.isEnabled();
    }
}
