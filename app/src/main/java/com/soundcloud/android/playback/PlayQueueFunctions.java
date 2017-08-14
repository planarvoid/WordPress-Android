package com.soundcloud.android.playback;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import io.reactivex.functions.Predicate;
import rx.functions.Func1;

public final class PlayQueueFunctions {

    public static final Predicate<CurrentPlayQueueItemEvent> IS_AUDIO_AD_QUEUE_ITEM = currentItemEvent -> currentItemEvent.getCurrentPlayQueueItem().isAudioAd();
    static final Func1<CurrentPlayQueueItemEvent, TrackQueueItem> TO_TRACK_QUEUE_ITEM = currentItemEvent -> (TrackQueueItem) currentItemEvent.getCurrentPlayQueueItem();

    private PlayQueueFunctions() {
    }
}
