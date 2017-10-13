package com.soundcloud.android.playback;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.playback.PlaybackService.Action;
import com.soundcloud.android.utils.Log;

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

    private PlaybackReceiver(PlaybackService playbackService, AccountOperations accountOperations,
                             PlayQueueManager playQueueManager) {
        this.playbackService = playbackService;
        this.accountOperations = accountOperations;
        this.playQueueManager = playQueueManager;
    }

    @Override
    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(PlaybackService.TAG, "BroadcastReceiver#onReceive(" + action + ")");

        if (Action.RESET_ALL.equals(action)) {
            playbackService.resetAll();
            playQueueManager.clearAll();

        } else if (accountOperations.isUserLoggedIn()) {
            if (Action.PLAY.equals(action)) {
                playbackService.play(getPlaybackItem(intent));
            } else if (Action.PRELOAD.equals(action)) {
                playbackService.preload(getPreloadItem(intent));
            } else if (Action.TOGGLE_PLAYBACK.equals(action)) {
                playbackService.togglePlayback();
            } else if (Action.RESUME.equals(action)) {
                playbackService.resume();
            } else if (Action.PAUSE.equals(action)) {
                playbackService.pause();
            } else if (Action.SEEK.equals(action)) {
                long seekPosition = getPositionFromIntent(intent);
                playbackService.seek(seekPosition);
            } else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
                if (playbackService.isPlayerPlaying()) {
                    Log.d(getClass().getSimpleName(), "Pausing from audio becoming noisy");
                    // avoid moving to paused state when unplugging headphones
                    // and not playing locally (i.e. when casting).
                    playbackService.pause();
                }
            } else if (Action.FADE_AND_PAUSE.equals(action)) {
                playbackService.fadeAndPause();
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

    private PreloadItem getPreloadItem(Intent intent) {
        return (PreloadItem) intent.getParcelableExtra(PlaybackService.ActionExtras.PRELOAD_ITEM);
    }

    static class Factory {
        private final PlayQueueManager playQueueManager;

        @Inject
        Factory(PlayQueueManager playQueueManager) {
            this.playQueueManager = playQueueManager;
        }

        PlaybackReceiver create(PlaybackService playbackService, AccountOperations accountOperations) {
            return new PlaybackReceiver(playbackService, accountOperations, playQueueManager);
        }

    }
}
