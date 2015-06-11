package com.soundcloud.android.playback.notification;

import com.soundcloud.android.NotificationConstants;
import com.soundcloud.propeller.PropertySet;

import android.app.Notification;
import android.app.NotificationManager;
import android.support.annotation.Nullable;

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
    public void clear() {
        notificationManager.cancel(NotificationConstants.PLAYBACK_NOTIFY_ID);
    }

    @Override
    @Nullable
    public Notification notifyPlaying() {
        return null;
    }

    @Override
    public boolean notifyIdleState() {
        return false;
    }
}
