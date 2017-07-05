package com.soundcloud.android.playback.widget;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackActionSource;
import com.soundcloud.android.playback.PlayerInteractionsTracker;
import com.soundcloud.android.playback.external.PlaybackAction;
import com.soundcloud.android.playback.external.PlaybackActionController;
import com.soundcloud.android.utils.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class WidgetPlaybackActionReceiver extends BroadcastReceiver {

    @Inject Controller controller;

    @Override
    public void onReceive(Context context, Intent intent) {
        SoundCloudApplication.getObjectGraph().inject(this);
        controller.handleIntent(intent);
    }

    static class Controller {

        private final PlaybackActionController playbackActionController;
        private final PlayerInteractionsTracker playerInteractionsTracker;
        private final PlaySessionStateProvider playSessionStateProvider;

        @Inject
        public Controller(PlaybackActionController playbackActionController,
                          PlayerInteractionsTracker playerInteractionsTracker,
                          PlaySessionStateProvider playSessionStateProvider) {
            this.playbackActionController = playbackActionController;
            this.playerInteractionsTracker = playerInteractionsTracker;
            this.playSessionStateProvider = playSessionStateProvider;
        }

        public void handleIntent(Intent intent) {
            final String action = intent.getAction();
            trackPlayerInteraction(action);
            playbackActionController.handleAction(action, PlaybackActionSource.WIDGET);
        }

        void trackPlayerInteraction(String action) {
            switch (action) {
                case PlaybackAction.NEXT:
                    playerInteractionsTracker.clickForward(PlaybackActionSource.WIDGET);
                    break;
                case PlaybackAction.PREVIOUS:
                    playerInteractionsTracker.clickBackward(PlaybackActionSource.WIDGET);
                    break;
                case PlaybackAction.TOGGLE_PLAYBACK:
                    if (playSessionStateProvider.isPlaying()) {
                        playerInteractionsTracker.pause(PlaybackActionSource.WIDGET);
                    } else {
                        playerInteractionsTracker.play(PlaybackActionSource.WIDGET);
                    }
                    break;
                default:
                    Log.i("Skipping tracking " + action);
                    break;
            }
        }
    }

}
