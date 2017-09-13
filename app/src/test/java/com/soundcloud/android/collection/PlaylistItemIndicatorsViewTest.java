package com.soundcloud.android.collection;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.view.DownloadImageView;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class PlaylistItemIndicatorsViewTest extends AndroidUnitTest {

    @Mock private FeatureFlags featureFlags;
    @Mock private OfflineSettingsOperations offlineSettingsOperations;
    @Mock private ConnectionHelper connectionHelper;
    @Mock private ChangeLikeToSaveExperiment changeLikeToSaveExperiment;

    private PlaylistItemIndicatorsView playlistItemIndicatorsView;
    private View view;

    @Before
    public void setUp() {
        playlistItemIndicatorsView = new PlaylistItemIndicatorsView(offlineSettingsOperations, connectionHelper, changeLikeToSaveExperiment);
        view = LayoutInflater.from(context()).inflate(R.layout.carousel_playlist_item_fixed_width, new FrameLayout(context()), false);
    }

    @Test
    public void shouldShowNoNetworkIndicatorOnRequestedAndNoWifi() {
        final DownloadImageView offlineIndicator = view.findViewById(R.id.offline_state_indicator);
        final ImageView noNetworkIndicator = view.findViewById(R.id.no_network_indicator);
        final View likeIndicator = view.findViewById(R.id.like_indicator);
        when(offlineSettingsOperations.isWifiOnlyEnabled()).thenReturn(true);
        when(connectionHelper.isWifiConnected()).thenReturn(false);

        playlistItemIndicatorsView.setupView(view, false, true, Optional.of(OfflineState.REQUESTED));

        assertThat(offlineIndicator).isGone();
        assertThat(noNetworkIndicator).isVisible();
        assertThat(likeIndicator).isGone();
    }

    @Test
    public void shouldShowNoNetworkIndicatorOnRequestedAndNoNetwork() {
        final DownloadImageView offlineIndicator = view.findViewById(R.id.offline_state_indicator);
        final ImageView noNetworkIndicator = view.findViewById(R.id.no_network_indicator);
        final View likeIndicator = view.findViewById(R.id.like_indicator);
        when(offlineSettingsOperations.isWifiOnlyEnabled()).thenReturn(false);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);

        playlistItemIndicatorsView.setupView(view, false, true, Optional.of(OfflineState.REQUESTED));

        assertThat(offlineIndicator).isGone();
        assertThat(noNetworkIndicator).isVisible();
        assertThat(likeIndicator).isGone();
    }

    @Test
    public void shouldShowOfflineIndicatorAndHideLikeIndicator() {
        final DownloadImageView offlineIndicator = view.findViewById(R.id.offline_state_indicator);
        final ImageView noNetworkIndicator = view.findViewById(R.id.no_network_indicator);
        final View likeIndicator = view.findViewById(R.id.like_indicator);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);

        playlistItemIndicatorsView.setupView(view, false, true, Optional.of(OfflineState.DOWNLOADED));

        assertThat(offlineIndicator).isVisible();
        assertThat(noNetworkIndicator).isGone();
        assertThat(likeIndicator).isGone();
    }

    @Test
    public void shouldHideOfflineIndicatorAndShowLikeIndicator() {
        final DownloadImageView offlineIndicator = view.findViewById(R.id.offline_state_indicator);
        final ImageView noNetworkIndicator = view.findViewById(R.id.no_network_indicator);
        final View likeIndicator = view.findViewById(R.id.like_indicator);

        playlistItemIndicatorsView.setupView(view, false, true, Optional.absent());

        assertThat(offlineIndicator).isGone();
        assertThat(noNetworkIndicator).isGone();
        assertThat(likeIndicator).isVisible();
    }

    @Test
    public void shouldHideOfflineIndicatorAndHideLikeIndicatorIfExperimentIsEnabled() {
        final DownloadImageView offlineIndicator = view.findViewById(R.id.offline_state_indicator);
        final ImageView noNetworkIndicator = view.findViewById(R.id.no_network_indicator);
        final View likeIndicator = view.findViewById(R.id.like_indicator);
        when(changeLikeToSaveExperiment.isEnabled()).thenReturn(true);

        playlistItemIndicatorsView.setupView(view, false, true, Optional.absent());

        assertThat(offlineIndicator).isGone();
        assertThat(noNetworkIndicator).isGone();
        assertThat(likeIndicator).isGone();
    }
}
