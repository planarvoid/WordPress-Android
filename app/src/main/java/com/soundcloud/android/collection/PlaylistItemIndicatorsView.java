package com.soundcloud.android.collection;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.view.DownloadImageView;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.java.optional.Optional;

import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.Arrays;

public class PlaylistItemIndicatorsView {

    @BindView(R.id.private_indicator) View privateIndicator;
    @BindView(R.id.offline_state_indicator) DownloadImageView offlineIndicator;
    @BindView(R.id.no_network_indicator) ImageView noNetworkIndicator;
    @BindView(R.id.like_indicator) View likeIndicator;

    private final OfflineSettingsOperations offlineSettingsOperations;
    private final ConnectionHelper connectionHelper;
    private final ChangeLikeToSaveExperiment changeLikeToSaveExperiment;

    @Inject
    PlaylistItemIndicatorsView(OfflineSettingsOperations offlineSettingsOperations,
                               ConnectionHelper connectionHelper,
                               ChangeLikeToSaveExperiment changeLikeToSaveExperiment) {
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
        ViewUtils.setGone(Arrays.asList(offlineIndicator, noNetworkIndicator, likeIndicator));
        showOfflineOrLikeIndicator(isLiked, offlineState);
    }

    private void showOfflineOrLikeIndicator(boolean isLiked, Optional<OfflineState> offlineState) {
        if (shouldShowOfflineIndicator(offlineState)) {
            showOfflineIndicator(offlineState.get());
        } else {
            offlineIndicator.setState(OfflineState.NOT_OFFLINE);
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
            offlineIndicator.setState(OfflineState.NOT_OFFLINE);
            noNetworkIndicator.setVisibility(View.VISIBLE);
        } else {
            offlineIndicator.setState(offlineState);
        }
    }

    private boolean shouldShowNoNetworkIndicator() {
        return (offlineSettingsOperations.isWifiOnlyEnabled() && !connectionHelper.isWifiConnected())
                || !connectionHelper.isNetworkConnected();
    }

    private boolean shouldShowLikeIndicator(boolean isLiked) {
        return isLiked && !changeLikeToSaveExperiment.isEnabled();
    }
}
