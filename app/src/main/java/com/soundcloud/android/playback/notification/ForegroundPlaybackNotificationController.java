package com.soundcloud.android.playback.notification;

import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.playback.PlaybackService;
import com.soundcloud.java.collections.PropertySet;

import android.app.NotificationManager;

import javax.inject.Inject;

class ForegroundPlaybackNotificationController implements PlaybackNotificationController.Strategy {

    private final NotificationManager notificationManager;

    @Inject
    public ForegroundPlaybackNotificationController(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    @Override
    public void setTrack(PropertySet track) {
        // no-op
    }

    @Override
    public void clear(PlaybackService playbackService) {
        playbackService.stopForeground(true);
        notificationManager.cancel(NotificationConstants.PLAYBACK_NOTIFY_ID);
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
