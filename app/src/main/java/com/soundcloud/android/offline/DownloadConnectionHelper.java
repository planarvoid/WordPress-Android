package com.soundcloud.android.offline;

import com.soundcloud.android.utils.ConnectionHelper;

import javax.inject.Inject;

class DownloadConnectionHelper {

    private final ConnectionHelper connectionHelper;
    private final OfflineSettingsStorage offlineSettings;

    @Inject
    DownloadConnectionHelper(ConnectionHelper connectionHelper, OfflineSettingsStorage offlineSettings) {
        this.connectionHelper = connectionHelper;
        this.offlineSettings = offlineSettings;
    }

    boolean isNetworkDisconnected() {
        return !connectionHelper.isNetworkConnected();
    }

    boolean isDownloadPermitted() {
        if (offlineSettings.isWifiOnlyEnabled()) {
            return connectionHelper.isWifiConnected();
        } else {
            return connectionHelper.isNetworkConnected();
        }
    }

}
