package com.soundcloud.android.offline;

import javax.inject.Inject;

public class OfflineSettingsOperations {

    private final OfflineSettingsStorage offlineSettingsStorage;

    @Inject
    public OfflineSettingsOperations(OfflineSettingsStorage offlineSettingsStorage) {
        this.offlineSettingsStorage = offlineSettingsStorage;
    }

    public boolean isWifiOnlyEnabled() {
        return offlineSettingsStorage.isWifiOnlyEnabled();
    }

}
