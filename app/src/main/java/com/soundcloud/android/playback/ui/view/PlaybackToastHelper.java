package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackResult;

import android.content.Context;
import android.widget.Toast;

import javax.inject.Inject;

public class PlaybackToastHelper {

    private final Context context;
    private final PlaySessionStateProvider playSessionStateProvider;

    @Inject
    public PlaybackToastHelper(Context context, PlaySessionStateProvider playSessionStateProvider) {
        this.context = context;
        this.playSessionStateProvider = playSessionStateProvider;
    }

    public void showToastOnPlaybackError(PlaybackResult.ErrorReason errorReason) {
        switch (errorReason) {
            case UNSKIPPABLE:
                showUnskippableAdToast();
                break;
            case TRACK_UNAVAILABLE_OFFLINE:
                showTrackUnavailableOfflineToast();
                break;
            case MISSING_PLAYABLE_TRACKS:
                showMissingPlayableTracksToast();
                break;
            case TRACK_UNAVAILABLE_CAST:
                showUnableToCastTrack();
                break;
            default:
                throw new IllegalStateException("Unknown error reason: " + errorReason);
        }
    }

    public void showUnskippableAdToast() {
        Toast.makeText(context, playSessionStateProvider.isPlaying()
                        ? R.string.ads_ad_in_progress
                        : R.string.ads_resume_playing_ad_to_continue,
                Toast.LENGTH_SHORT).show();
    }

    public void showTrackUnavailableOfflineToast() {
        Toast.makeText(context, R.string.offline_track_not_available, Toast.LENGTH_SHORT).show();
    }

    public void showMissingPlayableTracksToast() {
        Toast.makeText(context, R.string.playback_missing_playable_tracks, Toast.LENGTH_SHORT).show();
    }

    public void showConcurrentStreamingStoppedToast() {
        Toast.makeText(context, R.string.concurrent_streaming_stopped, Toast.LENGTH_LONG).show();
    }

    private void showUnableToCastTrack() {
        Toast.makeText(context, R.string.cast_unable_play_track, Toast.LENGTH_SHORT).show();
    }
}
