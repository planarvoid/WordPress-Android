package com.soundcloud.android.playback.service.mediaplayer;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.playback.service.PlaybackService;

import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;

@VisibleForTesting
class TrackCompletionListener implements MediaPlayer.OnCompletionListener {

    @VisibleForTesting
    static final int COMPLETION_TOLERANCE_MS = 3000;
    private MediaPlayerAdapter mediaPlayerAdapter;

    TrackCompletionListener(MediaPlayerAdapter mediaPlayerAdapter) {
        this.mediaPlayerAdapter = mediaPlayerAdapter;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

        // Check for a premature track completion that we should auto-retry
        final long lastPosition = getTargetStopPosition(mediaPlayer);

        if (shouldAutoRetry(lastPosition, mediaPlayer.getDuration())) {
            mediaPlayerAdapter.setResumeTimeAndInvokeErrorListener(mediaPlayer, lastPosition);

            Log.w(PlaybackService.TAG, "premature end of track [lastPosition = " + lastPosition
                    + ", duration = " + mediaPlayer.getDuration() + ", diff = "+ (mediaPlayer.getDuration() - lastPosition) + "]");

        } else if (mediaPlayerAdapter.isInErrorState()) {
            // onComplete must have been called in error state
            mediaPlayerAdapter.stop(mediaPlayer);

        } else {
            mediaPlayerAdapter.onTrackEnded();
        }
    }

    private boolean shouldAutoRetry(long lastPosition, long duration) {
        return mediaPlayerAdapter.isSeekable() && duration - lastPosition > COMPLETION_TOLERANCE_MS;
    }

    private long getTargetStopPosition(MediaPlayer mediaPlayer) {
        if (mediaPlayerAdapter.hasValidSeekPosition()){
            final long seekPos = mediaPlayerAdapter.getSeekPosition();
            Log.d(PlaybackService.TAG, "Calculating end pos from Seek position " + seekPos);
            return seekPos;

        } else if (mediaPlayerAdapter.isTryingToResumeTrack()){
            final long resumeTime = mediaPlayerAdapter.getResumeTime();
            Log.d(PlaybackService.TAG, "Calculating end pos from resume position " + resumeTime);
            return resumeTime;

        } else if ((Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) || mediaPlayerHasReset(mediaPlayer)) {
            // We are > JellyBean in which getCurrentPosition is totally unreliable or
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
        return mp.getCurrentPosition() <= 0 && mediaPlayerAdapter.isPlayerPlaying();
    }
}
