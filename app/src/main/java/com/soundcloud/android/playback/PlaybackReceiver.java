package com.soundcloud.android.playback;

import com.soundcloud.android.Consts;
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
    private static final long DEFAULT_DURATION = Consts.NOT_SET;

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

    @Override
    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
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
                playbackService.play(getPlaybackItem(intent));
            } else if (Action.TOGGLE_PLAYBACK.equals(action)) {
                playbackService.togglePlayback();
            } else if (Action.RESUME.equals(action)) {
                playbackService.play();
            } else if (Action.PAUSE.equals(action)) {
                playbackService.pause();
            } else if (Action.SEEK.equals(action)) {
                long seekPosition = getPositionFromIntent(intent);
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

    private long getPositionFromIntent(Intent intent) {
        return intent.getLongExtra(PlaybackService.ActionExtras.POSITION, DEFAULT_SEEK_POSITION);
    }

    private PlaybackItem getPlaybackItem(Intent intent) {
        return (PlaybackItem) intent.getParcelableExtra(PlaybackService.ActionExtras.PLAYBACK_ITEM);
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
        private final PlayQueueManager playQueueManager;

        @Inject
        Factory(PlayQueueManager playQueueManager) {
            this.playQueueManager = playQueueManager;
        }

        PlaybackReceiver create(PlaybackService playbackService, AccountOperations accountOperations,
                                EventBus eventBus) {
            return new PlaybackReceiver(playbackService, accountOperations, playQueueManager, eventBus);
        }

    }
}
