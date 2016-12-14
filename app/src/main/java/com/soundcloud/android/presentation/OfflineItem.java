package com.soundcloud.android.presentation;

import com.soundcloud.android.offline.OfflineState;

public interface OfflineItem {
    ListItem updatedWithOfflineState(OfflineState offlineState);
}
