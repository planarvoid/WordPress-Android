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
    private MediaPlayerAdapter mMediaPlayerAdapter;

    TrackCompletionListener(MediaPlayerAdapter mediaPlayerAdapter) {
        mMediaPlayerAdapter = mediaPlayerAdapter;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

        // Check for a premature track completion that we should auto-retry
        final long lastPosition = getTargetStopPosition(mediaPlayer);

        if (shouldAutoRetry(lastPosition, mediaPlayer.getDuration())) {
            mMediaPlayerAdapter.setResumeTimeAndInvokeErrorListener(mediaPlayer, lastPosition);

            Log.w(PlaybackService.TAG, "premature end of track [lastPosition = " + lastPosition
                    + ", duration = " + mediaPlayer.getDuration() + ", diff = "+ (mediaPlayer.getDuration() - lastPosition) + "]");

        } else if (mMediaPlayerAdapter.getLastStateTransition().wasError()) {
            // onComplete must have been called in error state
            mMediaPlayerAdapter.stop(mediaPlayer);

        } else {
            mMediaPlayerAdapter.onTrackEnded();
        }
    }

    private boolean shouldAutoRetry(long lastPosition, long duration) {
        return mMediaPlayerAdapter.isSeekable() && duration - lastPosition > COMPLETION_TOLERANCE_MS;
    }

    private long getTargetStopPosition(MediaPlayer mediaPlayer) {
        if (mMediaPlayerAdapter.hasValidSeekPosition()){
            final long seekPos = mMediaPlayerAdapter.getSeekPosition();
            Log.d(PlaybackService.TAG, "Calculating end pos from Seek position " + seekPos);
            return seekPos;

        } else if (mMediaPlayerAdapter.isTryingToResumeTrack()){
            final long resumeTime = mMediaPlayerAdapter.getResumeTime();
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
        return mp.getCurrentPosition() <= 0 && mMediaPlayerAdapter.getState().isPlayerPlaying();
    }
}
