package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.utils.NetworkConnectionHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DownloadConnexionHelperTest {
    @Mock private NetworkConnectionHelper connectionHelper;
    @Mock private OfflineSettingsStorage offlineSettings;

    private DownloadConnectionHelper helper;

    @Before
    public void setUp() throws Exception {
        when(connectionHelper.isWifiConnected()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
        helper = new DownloadConnectionHelper(connectionHelper, offlineSettings);
    }

    @Test
    public void invalidNetworkWhenDisconnected() {
        when(connectionHelper.isNetworkConnected()).thenReturn(false);

        assertThat(helper.isDownloadPermitted()).isFalse();
    }

    @Test
    public void validNetworkWhenConnectedAndAllNetworkAllowed() {
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(false);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);

        assertThat(helper.isDownloadPermitted()).isTrue();
    }

    @Test
    public void invalidNetworkWhenNotConnectedOnWifiAndOnlyWifiAllowed() {
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(true);
        when(connectionHelper.isWifiConnected()).thenReturn(false);

        assertThat(helper.isDownloadPermitted()).isFalse();
    }

    @Test
    public void validNetworkWhenConnectedOnWifiAndOnlyWifiAllowed() {
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(true);

        assertThat(helper.isDownloadPermitted()).isTrue();
    }
}
