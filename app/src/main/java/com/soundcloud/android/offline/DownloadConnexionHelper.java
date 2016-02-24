package com.soundcloud.android.offline;

import com.soundcloud.android.utils.NetworkConnectionHelper;

import javax.inject.Inject;

class DownloadConnexionHelper {

    private final NetworkConnectionHelper connectionHelper;
    private final OfflineSettingsStorage offlineSettings;

    @Inject
    DownloadConnexionHelper(NetworkConnectionHelper connectionHelper, OfflineSettingsStorage offlineSettings) {
        this.connectionHelper = connectionHelper;
        this.offlineSettings = offlineSettings;
    }

    boolean isNetworkDisconnected() {
        return !connectionHelper.isNetworkConnected();
    }

    boolean isNetworkDownloadFriendly() {
        if (offlineSettings.isWifiOnlyEnabled()) {
            return connectionHelper.isWifiConnected();
        } else {
            return connectionHelper.isNetworkConnected();
        }
    }

}
