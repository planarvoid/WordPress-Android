package com.soundcloud.android.playback.service;

import com.google.common.annotations.VisibleForTesting;

import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;

@VisibleForTesting
class TrackCompletionListener implements MediaPlayer.OnCompletionListener {

    @VisibleForTesting
    static final int COMPLETION_TOLERANCE_MS = 3000;
    private PlaybackService mPlaybackService;

    TrackCompletionListener(PlaybackService playbackService) {
        mPlaybackService = playbackService;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        logCompletionState();

        // Check for a premature track completion that we should auto-retry
        final long lastPosition = getTargetStopPosition(mp);

        if (shouldAutoRetry(lastPosition)) {

            final long currentTrackId = mPlaybackService.getPlayQueueInternal().getCurrentTrackId();
            final PlaybackProgressInfo resumeInfo = new PlaybackProgressInfo(currentTrackId, lastPosition);
            mPlaybackService.setResumeTimeAndInvokeErrorListener(mp, resumeInfo);

            final int duration = mPlaybackService.getDuration();
            Log.w(PlaybackService.TAG, "premature end of track [lastPosition = " + lastPosition
                    + ", duration = " + duration + ", diff = "+ (duration - lastPosition) + "]");

        } else if (mPlaybackService.getPlaybackStateInternal().isError()) {
            // onComplete must have been called in error state
            mPlaybackService.stop();

        } else {
            mPlaybackService.onTrackEnded();
        }
    }

    private void logCompletionState() {
        if (Log.isLoggable(PlaybackService.TAG, Log.DEBUG)) {
            Log.d(PlaybackService.TAG, "onCompletion(state=" + mPlaybackService.getPlaybackStateInternal() + ")");
        }
    }

    private boolean shouldAutoRetry(long lastPosition) {
        return mPlaybackService._isSeekable() && mPlaybackService.getDuration() - lastPosition > COMPLETION_TOLERANCE_MS;
    }

    private long getTargetStopPosition(MediaPlayer mp) {
        if (mPlaybackService.hasValidSeekPosition()){
            final long seekPos = mPlaybackService.getSeekPos();
            Log.d(PlaybackService.TAG, "Calculating end pos from Seek position " + seekPos);
            return seekPos;

        } else if (mPlaybackService.isTryingToResumeTrack()){
            final long resumeTime = mPlaybackService.getResumeTime();
            Log.d(PlaybackService.TAG, "Calculating end pos from resume position " + resumeTime);
            return resumeTime;

        } else if ((Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) || mediaPlayerHasReset(mp)) {
            // We are > JellyBean in which getCurrentPosition is totally unreliable or
            // mediaplayer seems to reset itself to 0 before this is called in certain builds, so pretend it's finished
            final int duration = mPlaybackService.getDuration();
            Log.d(PlaybackService.TAG, "Calculating end pos from completion position " + duration);
            return duration;

        } else {
            final int currentPosition = mp.getCurrentPosition();
            Log.d(PlaybackService.TAG, "Calculating end pos from current position " + currentPosition);
            return currentPosition;
        }
    }

    private boolean mediaPlayerHasReset(MediaPlayer mp) {
        return mp.getCurrentPosition() <= 0 && mPlaybackService.getPlaybackStateInternal() == PlaybackState.PLAYING;
    }
}
