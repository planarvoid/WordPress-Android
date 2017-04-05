package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineContentLocation.DEVICE_STORAGE;
import static com.soundcloud.android.offline.OfflineContentLocation.SD_CARD;

import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;

import javax.inject.Inject;
import java.io.File;

public class OfflineStorageOperations {

    private final Context context;
    private final CryptoOperations cryptoOperations;
    private final OfflineSettingsStorage offlineSettingsStorage;
    private final EventBus eventBus;

    @Inject
    OfflineStorageOperations(Context context, CryptoOperations cryptoOperations, OfflineSettingsStorage offlineSettingsStorage, EventBus eventBus) {
        this.context = context;
        this.cryptoOperations = cryptoOperations;
        this.offlineSettingsStorage = offlineSettingsStorage;
        this.eventBus = eventBus;
    }

    public void init() {
        checkForOfflineStorageConsistency();
        reportSdCardAvailability();
    }

    private void reportSdCardAvailability() {
        if (!offlineSettingsStorage.hasReportedSdCardAvailability()) {
            eventBus.publish(EventQueue.TRACKING, OfflineInteractionEvent.forSdCardAvailable(IOUtils.isSDCardMounted(context)));
            offlineSettingsStorage.setSdCardAvailabilityReported();
        }
    }

    private void checkForOfflineStorageConsistency() {
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
