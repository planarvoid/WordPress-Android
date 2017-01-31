package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.PlaylistEngagementsRenderer.PlaylistDetailInfoProvider;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlaylistDetailInfoProviderTest extends AndroidUnitTest {

    private static final String DEFAULT_TEXT = "default-text";

    private PlaylistDetailInfoProvider infoProvider;
    @Mock private OfflineSettingsOperations offlineSettings;
    @Mock private NetworkConnectionHelper connectionHelper;

    @Before
    public void setUp() throws Exception {
        infoProvider = new PlaylistDetailInfoProvider(offlineSettings, connectionHelper, resources());
    }

    @Test
    public void returnsInfoLabelWhenNotRequestedForOffline() throws Exception {
        assertThat(infoProvider.getPlaylistInfoLabel(OfflineState.NOT_OFFLINE, DEFAULT_TEXT)).isEqualTo(DEFAULT_TEXT);
    }

    @Test
    public void returnsInfoLabelWhenDownloaded() throws Exception {
        assertThat(infoProvider.getPlaylistInfoLabel(OfflineState.DOWNLOADED, DEFAULT_TEXT)).isEqualTo(DEFAULT_TEXT);
    }

    @Test
    public void returnsInfoLabelWhenDownloading() throws Exception {
        assertThat(infoProvider.getPlaylistInfoLabel(OfflineState.DOWNLOADING, DEFAULT_TEXT)).isEqualTo(DEFAULT_TEXT);
    }

    @Test
    public void returnsInfoLabelWhenUnavailable() throws Exception {
        assertThat(infoProvider.getPlaylistInfoLabel(OfflineState.UNAVAILABLE, DEFAULT_TEXT)).isEqualTo(DEFAULT_TEXT);
    }

    @Test
    public void returnsInfoLabelWhenRequestedWithConnection() throws Exception {
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(false);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);

        assertThat(infoProvider.getPlaylistInfoLabel(OfflineState.REQUESTED, DEFAULT_TEXT)).isEqualTo(DEFAULT_TEXT);
    }

    @Test
    public void returnsOfflineNoWifiWhenRequestedWithWifiOnlyAndWithoutWifi() throws Exception {
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(true);
        when(connectionHelper.isWifiConnected()).thenReturn(false);

        assertThat(infoProvider.getPlaylistInfoLabel(OfflineState.REQUESTED, DEFAULT_TEXT)).isEqualTo("No Wi-Fi");
    }

    @Test
    public void returnsOfflineWhenRequestedWithWifiOnlyAndWithoutWifi() throws Exception {
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(false);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);

        assertThat(infoProvider.getPlaylistInfoLabel(OfflineState.REQUESTED, DEFAULT_TEXT)).isEqualTo("No connection");
    }
}