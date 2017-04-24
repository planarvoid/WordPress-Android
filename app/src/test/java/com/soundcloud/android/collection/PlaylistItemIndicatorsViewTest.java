package com.soundcloud.android.collection;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
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

    private PlaylistItemIndicatorsView playlistItemIndicatorsView;
    private View view;

    @Before
    public void setUp() {
        playlistItemIndicatorsView = new PlaylistItemIndicatorsView(featureFlags, offlineSettingsOperations, connectionHelper);
        view = LayoutInflater.from(context()).inflate(R.layout.carousel_playlist_item_fixed_width, new FrameLayout(context()), false);
    }

    @Test
    public void shouldSetupOldOfflineAndLikeIndicators() {
        final DownloadImageView oldOfflineIndicator = (DownloadImageView) view.findViewById(R.id.item_download_state);
        final View likeIndicator = view.findViewById(R.id.like_indicator);

        when(featureFlags.isEnabled(Flag.NEW_OFFLINE_ICONS)).thenReturn(false);

        playlistItemIndicatorsView.setupView(view, false, true, Optional.of(OfflineState.DOWNLOADED));

        assertThat(oldOfflineIndicator).isVisible();
        assertThat(likeIndicator).isVisible();
    }

    @Test
    public void shouldShowNoNetworkIndicatorOnRequestedAndNoWifi() {
        final DownloadImageView offlineIndicator = (DownloadImageView) view.findViewById(R.id.offline_state_indicator);
        final ImageView noNetworkIndicator = (ImageView) view.findViewById(R.id.no_network_indicator);
        final View likeIndicator = view.findViewById(R.id.like_indicator);

        when(featureFlags.isEnabled(Flag.NEW_OFFLINE_ICONS)).thenReturn(true);
        when(offlineSettingsOperations.isWifiOnlyEnabled()).thenReturn(true);
        when(connectionHelper.isWifiConnected()).thenReturn(false);

        playlistItemIndicatorsView.setupView(view, false, true, Optional.of(OfflineState.REQUESTED));

        assertThat(offlineIndicator).isGone();
        assertThat(noNetworkIndicator).isVisible();
        assertThat(likeIndicator).isGone();
    }

    @Test
    public void shouldShowNoNetworkIndicatorOnRequestedAndNoNetwork() {
        final DownloadImageView offlineIndicator = (DownloadImageView) view.findViewById(R.id.offline_state_indicator);
        final ImageView noNetworkIndicator = (ImageView) view.findViewById(R.id.no_network_indicator);
        final View likeIndicator = view.findViewById(R.id.like_indicator);

        when(featureFlags.isEnabled(Flag.NEW_OFFLINE_ICONS)).thenReturn(true);
        when(offlineSettingsOperations.isWifiOnlyEnabled()).thenReturn(false);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);

        playlistItemIndicatorsView.setupView(view, false, true, Optional.of(OfflineState.REQUESTED));

        assertThat(offlineIndicator).isGone();
        assertThat(noNetworkIndicator).isVisible();
        assertThat(likeIndicator).isGone();
    }

    @Test
    public void shouldShowOfflineIndicatorAndHideLikeIndicator() {
        final DownloadImageView offlineIndicator = (DownloadImageView) view.findViewById(R.id.offline_state_indicator);
        final ImageView noNetworkIndicator = (ImageView) view.findViewById(R.id.no_network_indicator);
        final View likeIndicator = view.findViewById(R.id.like_indicator);

        when(featureFlags.isEnabled(Flag.NEW_OFFLINE_ICONS)).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);

        playlistItemIndicatorsView.setupView(view, false, true, Optional.of(OfflineState.DOWNLOADED));

        assertThat(offlineIndicator).isVisible();
        assertThat(noNetworkIndicator).isGone();
        assertThat(likeIndicator).isGone();
    }

    @Test
    public void shouldHideOfflineIndicatorAndShowLikeIndicator() {
        final DownloadImageView offlineIndicator = (DownloadImageView) view.findViewById(R.id.offline_state_indicator);
        final ImageView noNetworkIndicator = (ImageView) view.findViewById(R.id.no_network_indicator);
        final View likeIndicator = view.findViewById(R.id.like_indicator);

        when(featureFlags.isEnabled(Flag.NEW_OFFLINE_ICONS)).thenReturn(true);

        playlistItemIndicatorsView.setupView(view, false, true, Optional.absent());

        assertThat(offlineIndicator).isGone();
        assertThat(noNetworkIndicator).isGone();
        assertThat(likeIndicator).isVisible();
    }
}
