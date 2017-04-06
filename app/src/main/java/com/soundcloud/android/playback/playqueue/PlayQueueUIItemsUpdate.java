package com.soundcloud.android.playback.playqueue;

import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class PlayQueueUIItemsUpdate {

    private static final int QUEUE_LOAD = 0;
    private static final int ITEM_ADDED = 1;
    private static final int QUEUE_REORDER = 2;
    private static final int TRACK_CHANGED = 3;

    public abstract List<PlayQueueUIItem> items();

    public abstract int kind();

    public static PlayQueueUIItemsUpdate forQueueLoad() {
        return new AutoValue_PlayQueueUIItemsUpdate(Collections.emptyList(), QUEUE_LOAD);
    }

    public static PlayQueueUIItemsUpdate forItemAdded() {
        return new AutoValue_PlayQueueUIItemsUpdate(Collections.emptyList(), ITEM_ADDED);
    }

    public static PlayQueueUIItemsUpdate forQueueReorder() {
        return new AutoValue_PlayQueueUIItemsUpdate(Collections.emptyList(), QUEUE_REORDER);
    }

    public static PlayQueueUIItemsUpdate forTrackChanged() {
        return new AutoValue_PlayQueueUIItemsUpdate(Collections.emptyList(), TRACK_CHANGED);
    }

    public PlayQueueUIItemsUpdate withItems(List<PlayQueueUIItem> items) {
        return new AutoValue_PlayQueueUIItemsUpdate(items, kind());
    }

    public boolean isQueueLoad() {
        return kind() == QUEUE_LOAD;
    }

    public boolean isItemAdded() {
        return kind() == ITEM_ADDED;
    }

    public boolean isQueueReorder() {
        return kind() == QUEUE_REORDER;
    }

    public boolean isTrackChanged() {
        return kind() == TRACK_CHANGED;
    }

}
