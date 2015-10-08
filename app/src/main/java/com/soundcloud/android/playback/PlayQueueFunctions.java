package com.soundcloud.android.playback;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;

import rx.functions.Func1;

public final class PlayQueueFunctions {

    private PlayQueueFunctions() {}

    public static final Func1<CurrentPlayQueueItemEvent, Boolean> IS_TRACK_QUEUE_ITEM = new Func1<CurrentPlayQueueItemEvent, Boolean>() {
        @Override
        public Boolean call(CurrentPlayQueueItemEvent currentItemEvent) {
            return currentItemEvent.getCurrentPlayQueueItem().isTrack();
        }
    };

    public static final Func1<CurrentPlayQueueItemEvent, TrackQueueItem> TO_TRACK_QUEUE_ITEM = new Func1<CurrentPlayQueueItemEvent, TrackQueueItem>() {
        @Override
        public TrackQueueItem call(CurrentPlayQueueItemEvent currentItemEvent) {
            return (TrackQueueItem) currentItemEvent.getCurrentPlayQueueItem();
        }
    };
}
