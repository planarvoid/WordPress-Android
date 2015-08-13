package com.soundcloud.android.offline;

import javax.inject.Inject;

public class OfflineSettingsOperations {

    private final OfflineSettingsStorage offlineSettingsStorage;

    @Inject
    public OfflineSettingsOperations(OfflineSettingsStorage offlineSettingsStorage) {
        this.offlineSettingsStorage = offlineSettingsStorage;
    }

    public boolean hasOfflineContent() {
        return offlineSettingsStorage.hasOfflineContent();
    }

    public void setHasOfflineContent(boolean hasOfflineContent) {
        offlineSettingsStorage.setHasOfflineContent(hasOfflineContent);
    }

}
