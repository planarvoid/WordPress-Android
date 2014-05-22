package com.soundcloud.android.playback.widget;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.playback.service.PlaybackService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

/**
 * Handles track likes initiated from the widget's remote views.
 */
public class PlayerWidgetReceiver extends BroadcastReceiver {

    @Inject
    PlayerWidgetController controller;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (PlaybackService.Actions.WIDGET_LIKE_CHANGED.equals(intent.getAction())) {
            SoundCloudApplication.getObjectGraph().inject(this);
            boolean isLiked = intent.getBooleanExtra(PlaybackService.BroadcastExtras.IS_LIKE, false);
            controller.handleToggleLikeAction(isLiked);
        }
    }

}
