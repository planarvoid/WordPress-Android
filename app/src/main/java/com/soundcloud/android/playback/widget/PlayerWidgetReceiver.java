package com.soundcloud.android.playback.widget;

import com.soundcloud.android.SoundCloudApplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

/**
 * Handles track likes initiated from the widget's remote views.
 */
public class PlayerWidgetReceiver extends BroadcastReceiver {

    @Inject PlayerWidgetController controller;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (PlayerWidgetController.ACTION_LIKE_CHANGED.equals(intent.getAction())) {
            SoundCloudApplication.getObjectGraph().inject(this);
            boolean isLike = intent.getBooleanExtra(PlayerWidgetController.EXTRA_IS_LIKE, false);
            controller.handleToggleLikeAction(isLike);
        }
    }

}
