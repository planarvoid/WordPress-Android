package com.soundcloud.android.playback;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackService.Action;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import javax.inject.Inject;

class PlaybackReceiver extends BroadcastReceiver {

    private static final long DEFAULT_SEEK_POSITION = 0L;

    private final PlaybackService playbackService;
    private final PlayQueueManager playQueueManager;
    private final AccountOperations accountOperations;
    private final EventBus eventBus;

    private PlaybackReceiver(PlaybackService playbackService, AccountOperations accountOperations,
                            PlayQueueManager playQueueManager, EventBus eventBus) {
        this.playbackService = playbackService;
        this.accountOperations = accountOperations;
        this.playQueueManager = playQueueManager;
        this.eventBus = eventBus;
    }

    @Override @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(PlaybackService.TAG, "BroadcastReceiver#onReceive(" + action + ")");

        if (intent.hasExtra(PlayControlEvent.EXTRA_EVENT_SOURCE)) {
            trackPlayControlEvent(intent);
        }

        if (Action.RESET_ALL.equals(action)) {
            playbackService.resetAll();
            playQueueManager.clearAll();

        } else if (accountOperations.isUserLoggedIn()) {
            if (Action.PLAY.equals(action)) {
                playbackService.play(getUrnFromIntent(intent), 0);
            } else if (Action.PLAY_UNINTERRUPTED.equals(action)) {
                playbackService.playUninterrupted(getUrnFromIntent(intent));
            } else if (Action.PLAY_OFFLINE.equals(action)) {
                playbackService.playOffline(getUrnFromIntent(intent), 0);
            } else if (Action.TOGGLE_PLAYBACK.equals(action)) {
                playbackService.togglePlayback();
            } else if (Action.RESUME.equals(action)) {
                playbackService.play();
            } else if (Action.PAUSE.equals(action)) {
                playbackService.pause();
            } else if (Action.SEEK.equals(action)) {
                long seekPosition = intent.getLongExtra(PlaybackService.ActionExtras.POSITION, DEFAULT_SEEK_POSITION);
                playbackService.seek(seekPosition, true);
            } else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
                playbackService.pause();

            } else if (Action.STOP.equals(action)) {
                // make sure we go to a stopped stat. No-op if there already
                playbackService.stop();
            }
        } else {
            Log.e(PlaybackService.TAG, "Aborting playback service action, no soundcloud account");
        }
    }

    private Urn getUrnFromIntent(Intent intent) {
        return (Urn) intent.getParcelableExtra(PlaybackService.ActionExtras.URN);
    }

    private void trackPlayControlEvent(Intent intent) {
        String source = intent.getStringExtra(PlayControlEvent.EXTRA_EVENT_SOURCE);

        if (Action.RESUME.equals(intent.getAction())) {
            eventBus.publish(EventQueue.TRACKING, PlayControlEvent.play(source));
        } else if (Action.PAUSE.equals(intent.getAction())) {
            eventBus.publish(EventQueue.TRACKING, PlayControlEvent.pause(source));
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
