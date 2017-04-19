package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.PlaylistEngagementsRenderer.PlaylistDetailInfoProvider;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.ConnectionHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlaylistDetailInfoProviderTest extends AndroidUnitTest {

    private static final String DEFAULT_TEXT = "default-text";

    @Mock private OfflineSettingsOperations offlineSettings;
    @Mock private ConnectionHelper connectionHelper;
    @Mock private FeatureFlags featureFlags;

    private PlaylistDetailInfoProvider infoProvider;

    @Before
    public void setUp() throws Exception {
        infoProvider = new PlaylistDetailInfoProvider(offlineSettings, connectionHelper, resources(), featureFlags);
    }

    @Test
    public void returnsInfoLabelWhenNotRequestedForOffline() {
        assertThat(infoProvider.getPlaylistInfoLabel(OfflineState.NOT_OFFLINE, DEFAULT_TEXT)).isEqualTo(DEFAULT_TEXT);
    }

    @Test
    public void returnsInfoLabelWhenDownloaded() {
        assertThat(infoProvider.getPlaylistInfoLabel(OfflineState.DOWNLOADED, DEFAULT_TEXT)).isEqualTo(DEFAULT_TEXT);
    }

    @Test
    public void returnsInfoLabelWhenDownloading() {
        assertThat(infoProvider.getPlaylistInfoLabel(OfflineState.DOWNLOADING, DEFAULT_TEXT)).isEqualTo(DEFAULT_TEXT);
    }

    @Test
    public void returnsInfoLabelWhenUnavailable() {
        assertThat(infoProvider.getPlaylistInfoLabel(OfflineState.UNAVAILABLE, DEFAULT_TEXT)).isEqualTo(DEFAULT_TEXT);
    }

    @Test
    public void returnsInfoLabelWhenRequestedWithConnection() {
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(false);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);

        assertThat(infoProvider.getPlaylistInfoLabel(OfflineState.REQUESTED, DEFAULT_TEXT)).isEqualTo(DEFAULT_TEXT);
    }

    @Test
    public void returnsOfflineNoWifiWhenRequestedWithWifiOnlyAndWithoutWifi() {
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(true);
        when(connectionHelper.isWifiConnected()).thenReturn(false);
        when(featureFlags.isDisabled(Flag.NEW_OFFLINE_ICONS)).thenReturn(true);

        assertThat(infoProvider.getPlaylistInfoLabel(OfflineState.REQUESTED, DEFAULT_TEXT)).isEqualTo("No Wi-Fi");
    }

    @Test
    public void returnsOfflineWhenRequestedWithWifiOnlyAndWithoutWifi() {
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(false);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        when(featureFlags.isDisabled(Flag.NEW_OFFLINE_ICONS)).thenReturn(true);

        assertThat(infoProvider.getPlaylistInfoLabel(OfflineState.REQUESTED, DEFAULT_TEXT)).isEqualTo("No connection");
    }

    @Test
    public void returnsDefaultTextWithNewOfflineIconsFlagEnabled() {
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(false);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        when(featureFlags.isDisabled(Flag.NEW_OFFLINE_ICONS)).thenReturn(false);

        assertThat(infoProvider.getPlaylistInfoLabel(OfflineState.REQUESTED, DEFAULT_TEXT)).isEqualTo(DEFAULT_TEXT);
    }

}
