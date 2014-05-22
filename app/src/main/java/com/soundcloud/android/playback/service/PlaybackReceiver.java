package com.soundcloud.android.playback.service;

import static com.soundcloud.android.playback.service.PlaybackService.Actions;
import static com.soundcloud.android.playback.service.PlaybackService.Broadcasts;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.utils.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import javax.inject.Inject;

class PlaybackReceiver extends BroadcastReceiver {

    private PlaybackService playbackService;
    private final AccountOperations accountOperations;
    private final PlayQueueManager playQueueManager;
    private final EventBus eventBus;

    private PlaybackReceiver(PlaybackService playbackService, AccountOperations accountOperations,
                            PlayQueueManager playQueueManager, EventBus eventBus) {
        this.playbackService = playbackService;
        this.accountOperations = accountOperations;
        this.playQueueManager = playQueueManager;
        this.eventBus = eventBus;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(PlaybackService.TAG, "BroadcastReceiver#onReceive(" + action + ")");

        if (intent.hasExtra(PlayControlEvent.EXTRA_EVENT_SOURCE)) {
            trackPlayControlEvent(intent);
        }

        if (Actions.RESET_ALL.equals(action)) {
            playbackService.resetAll();
            playQueueManager.clearAll();

        } else if (accountOperations.isUserLoggedIn()) {
            if (Actions.PLAY_CURRENT.equals(action)) {
                playbackService.openCurrent();
            } else if (Actions.TOGGLEPLAYBACK_ACTION.equals(action)) {
                playbackService.togglePlayback();
            } else if (Actions.PLAY_ACTION.equals(action)) {
                playbackService.play();
            } else if (Actions.PAUSE_ACTION.equals(action)) {
                playbackService.pause();
            } else if (Broadcasts.UPDATE_WIDGET_ACTION.equals(action)) {
                // a widget was just added. Fake a playstate changed so it gets updated
                playbackService.notifyChange(Broadcasts.PLAYSTATE_CHANGED);
            } else if (Actions.RETRY_RELATED_TRACKS.equals(action)) {
                playQueueManager.retryRelatedTracksFetch();
            } else if (Broadcasts.PLAYQUEUE_CHANGED.equals(action)) {
                if (playbackService.isWaitingForPlaylist()) {
                    playbackService.openCurrent();
                }
            } else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
                playbackService.pause();

            } else if (Actions.STOP_ACTION.equals(action)) {
                // make sure we go to a stopped stat. No-op if there already
                playbackService.stop();
            }
        } else {
            Log.e(PlaybackService.TAG, "Aborting playback service action, no soundcloud account");
        }
    }

    private void trackPlayControlEvent(Intent intent) {
        String source = intent.getStringExtra(PlayControlEvent.EXTRA_EVENT_SOURCE);

        if (Actions.PLAY_ACTION.equals(intent.getAction())) {
            eventBus.publish(EventQueue.PLAY_CONTROL, PlayControlEvent.play(source));
        } else if (Actions.PAUSE_ACTION.equals(intent.getAction())) {
            eventBus.publish(EventQueue.PLAY_CONTROL, PlayControlEvent.pause(source));
        }
    }

    static class Factory {
        @Inject
        Factory() { }

        PlaybackReceiver create(PlaybackService playbackService, AccountOperations accountOperations,
                                PlayQueueManager playQueueManager, EventBus eventBus) {
            return new PlaybackReceiver(playbackService, accountOperations, playQueueManager, eventBus);
        }

    }
}
