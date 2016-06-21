package com.soundcloud.android.playback;

import com.soundcloud.android.events.PlaybackProgressEvent;

public interface PlaybackAnalyticsDispatcher {

    void onProgressEvent(PlaybackProgressEvent progressEvent);

    void onPlayTransition(PlaybackStateTransition transition, boolean isNewItem);

    void onStopTransition(PlaybackStateTransition transition, boolean isNewItem);

    void onSkipTransition(PlaybackStateTransition transition);

    void onProgressCheckpoint(PlaybackStateTransition previousTransition, PlaybackProgressEvent progressEvent);

}
