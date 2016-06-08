package com.soundcloud.android.playback;

import com.soundcloud.android.events.PlaybackProgressEvent;

public interface PlaybackAnalyticsDispatcher {

    void onProgressEvent(PlaybackProgressEvent progressEvent);

    void onPlayTransition(PlaybackStateTransition transition, boolean isNewItem);

    void onStopTransition(PlaybackStateTransition transition);

    void onSkipTransition(PlaybackStateTransition transition);

}
