package com.soundcloud.android.playback;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;

import rx.functions.Func1;

public final class PlayQueueFunctions {

    private PlayQueueFunctions() {
    }

    public static final Func1<CurrentPlayQueueItemEvent, Boolean> IS_AUDIO_AD_QUEUE_ITEM = currentItemEvent -> currentItemEvent.getCurrentPlayQueueItem().isAudioAd();

    public static final Func1<CurrentPlayQueueItemEvent, TrackQueueItem> TO_TRACK_QUEUE_ITEM = currentItemEvent -> (TrackQueueItem) currentItemEvent.getCurrentPlayQueueItem();
}
