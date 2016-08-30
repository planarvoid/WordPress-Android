package com.soundcloud.android.playback;

import com.soundcloud.android.events.PlaybackProgressEvent;

public interface PlaybackAnalyticsDispatcher {

    void onProgressEvent(PlaybackProgressEvent progressEvent);

    void onPlayTransition(PlayStateEvent playStateEvent, boolean isNewItem);

    void onStopTransition(PlayStateEvent playStateEvent, boolean isNewItem);

    void onSkipTransition(PlayStateEvent playStateEvent);

    void onProgressCheckpoint(PlayStateEvent previousPlayStateEvent, PlaybackProgressEvent progressEvent);

}
