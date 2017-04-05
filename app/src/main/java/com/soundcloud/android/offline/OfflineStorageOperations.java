package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineContentLocation.DEVICE_STORAGE;
import static com.soundcloud.android.offline.OfflineContentLocation.SD_CARD;

import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.utils.IOUtils;

import android.content.Context;

import javax.inject.Inject;
import java.io.File;

public class OfflineStorageOperations {

    private final CryptoOperations cryptoOperations;
    private final OfflineSettingsStorage offlineSettingsStorage;
    private final Context context;

    @Inject
    OfflineStorageOperations(CryptoOperations cryptoOperations,
                             OfflineSettingsStorage offlineSettingsStorage,
                             Context context) {
        this.cryptoOperations = cryptoOperations;
        this.offlineSettingsStorage = offlineSettingsStorage;
        this.context = context;
    }

    public void checkForOfflineStorageConsistency(Context context) {
        if (IOUtils.isSDCardMounted(context) && shouldDeleteOfflineDirOnSDCard()) {
            File sdCardDir = IOUtils.getSDCardDir(context);
            if (sdCardDir != null) {
                IOUtils.cleanDir(sdCardDir);
            }
        }
    }

    void updateOfflineContentOnSdCard() {
        if (SD_CARD == offlineSettingsStorage.getOfflineContentLocation()) {
            OfflineContentService.start(context);
        }
    }

    private boolean shouldDeleteOfflineDirOnSDCard() {
        return !cryptoOperations.containsDeviceKey() || DEVICE_STORAGE == offlineSettingsStorage.getOfflineContentLocation();
    }
}