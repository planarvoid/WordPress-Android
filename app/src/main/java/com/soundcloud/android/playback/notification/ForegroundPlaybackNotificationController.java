package com.soundcloud.android.playback.notification;

import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.propeller.PropertySet;

import javax.inject.Inject;

class ForegroundPlaybackNotificationController implements PlaybackNotificationController.Strategy {

    @Inject
    public ForegroundPlaybackNotificationController() {
    }

    @Override
    public void setTrack(PropertySet track) {
        // no-op
    }

    @Override
    public void clear(PlaybackService playbackService) {
        final boolean removeNotification = true;
        playbackService.stopForeground(removeNotification);
    }

    @Override
    public void notifyPlaying(PlaybackService playbackService) {
        // no-op
    }

    @Override
    public void notifyIdleState(PlaybackService playbackService) {
        // no-op
    }
}
