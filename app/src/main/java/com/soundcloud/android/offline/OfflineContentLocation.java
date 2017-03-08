package com.soundcloud.android.offline;

import android.support.annotation.Nullable;

public enum OfflineContentLocation {

    DEVICE_STORAGE("device_storage"),
    SD_CARD("sd_card");

    public final String id;

    OfflineContentLocation(String id) {
        this.id = id;
    }

    public static OfflineContentLocation fromId(@Nullable String id) {
        if (SD_CARD.id.equals(id)) {
            return SD_CARD;
        }
        return DEVICE_STORAGE;
    }
}
