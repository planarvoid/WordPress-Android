package com.soundcloud.android.collection;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.DownloadImageView;
import com.soundcloud.java.optional.Optional;

import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.Arrays;

public class PlaylistItemIndicatorsView {

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

        ButterKnife.findById(view, R.id.private_indicator).setVisibility(isPrivate ? View.VISIBLE : View.GONE);

        ImageView noNetworkIndicator = ButterKnife.findById(view, R.id.no_network_indicator);
        View likeIndicator = ButterKnife.findById(view, R.id.like_indicator);
        DownloadImageView offlineIndicator = ButterKnife.findById(view, R.id.offline_state_indicator);

        ViewUtils.setGone(Arrays.asList(offlineIndicator, noNetworkIndicator, likeIndicator));
        if (shouldShowOfflineIndicator(offlineState)) {
            showOfflineIndicator(noNetworkIndicator, offlineIndicator, offlineState.get());
        } else {
            offlineIndicator.setState(OfflineState.NOT_OFFLINE);
            likeIndicator.setVisibility(shouldShowLikeIndicator(isLiked)
                                        ? View.VISIBLE
                                        : View.GONE);
        }
    }

    private void showOfflineIndicator(ImageView noNetworkIndicator, DownloadImageView offlineIndicator, OfflineState offlineState) {
        if (OfflineState.REQUESTED == offlineState && shouldShowNoNetworkIndicator()) {
            offlineIndicator.setState(OfflineState.NOT_OFFLINE);
            noNetworkIndicator.setVisibility(View.VISIBLE);
        } else {
            offlineIndicator.setState(offlineState);
        }
    }

    private boolean shouldShowOfflineIndicator(Optional<OfflineState> offlineState) {
        return offlineState.isPresent() && OfflineState.NOT_OFFLINE != offlineState.get();
    }

    private boolean shouldShowNoNetworkIndicator() {
        return (offlineSettingsOperations.isWifiOnlyEnabled() && !connectionHelper.isWifiConnected())
                || !connectionHelper.isNetworkConnected();
    }

    private boolean shouldShowLikeIndicator(boolean isLiked) {
        return isLiked && !changeLikeToSaveExperiment.isEnabled();
    }
}
