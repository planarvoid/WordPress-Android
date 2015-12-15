package com.soundcloud.android.playback.mediaplayer;

import com.soundcloud.android.playback.PlaybackService;

import android.media.MediaPlayer;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

@VisibleForTesting
class TrackCompletionListener implements MediaPlayer.OnCompletionListener {

    @VisibleForTesting
    /* package */ static final int COMPLETION_TOLERANCE_MS = 3000;
    private final MediaPlayerAudioAdapter mediaPlayerAudioAdapter;

    TrackCompletionListener(MediaPlayerAudioAdapter mediaPlayerAudioAdapter) {
        this.mediaPlayerAudioAdapter = mediaPlayerAudioAdapter;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

        // Check for a premature track completion that we should auto-retry
        final long lastPosition = getTargetStopPosition(mediaPlayer);
        final int duration = mediaPlayer.getDuration();

        if (shouldAutoRetry(lastPosition, duration)) {
            mediaPlayerAudioAdapter.setResumeTimeAndInvokeErrorListener(mediaPlayer, lastPosition);

            Log.w(PlaybackService.TAG, "premature end of track [lastPosition = " + lastPosition
                    + ", duration = " + duration + ", diff = "+ (duration - lastPosition) + "]");

        } else if (mediaPlayerAudioAdapter.isInErrorState()) {
            // onComplete must have been called in error state
            mediaPlayerAudioAdapter.stop(mediaPlayer);

        } else {
            mediaPlayerAudioAdapter.onTrackEnded();
        }
    }

    private boolean shouldAutoRetry(long lastPosition, long duration) {
        return mediaPlayerAudioAdapter.isSeekable() && duration - lastPosition > COMPLETION_TOLERANCE_MS;
    }

    private long getTargetStopPosition(MediaPlayer mediaPlayer) {
        if (mediaPlayerAudioAdapter.hasValidSeekPosition()){
            final long seekPos = mediaPlayerAudioAdapter.getSeekPosition();
            Log.d(PlaybackService.TAG, "Calculating end pos from Seek position " + seekPos);
            return seekPos;

        } else if (mediaPlayerAudioAdapter.isTryingToResumeTrack()){
            final long resumeTime = mediaPlayerAudioAdapter.getResumeTime();
            Log.d(PlaybackService.TAG, "Calculating end pos from resume position " + resumeTime);
            return resumeTime;

        } else if (mediaPlayerHasReset(mediaPlayer)) {
            // mediaplayer seems to reset itself to 0 before this is called in certain builds, so pretend it's finished
            final long duration = mediaPlayer.getDuration();
            Log.d(PlaybackService.TAG, "Calculating end pos from completion position " + duration);
            return duration;

        } else {
            final int currentPosition = mediaPlayer.getCurrentPosition();
            Log.d(PlaybackService.TAG, "Calculating end pos from current position " + currentPosition);
            return currentPosition;
        }
    }

    private boolean mediaPlayerHasReset(MediaPlayer mp) {
        return mp.getCurrentPosition() <= 0 && mediaPlayerAudioAdapter.isPlayerPlaying();
    }
}
