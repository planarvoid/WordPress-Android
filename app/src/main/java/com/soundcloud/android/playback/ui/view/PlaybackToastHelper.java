package com.soundcloud.android.playback.ui.view;

import static com.soundcloud.android.offline.OfflinePlaybackOperations.TrackNotAvailableOffline;
import static com.soundcloud.android.playback.PlaybackOperations.UnskippablePeriodException;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaySessionStateProvider;

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

    public void showUnskippableAdToast() {
        Toast.makeText(context, playSessionStateProvider.isPlaying()
                        ? R.string.ad_in_progress
                        : R.string.ad_resume_playing_to_continue,
                Toast.LENGTH_SHORT).show();
    }

    private void showTrackUnavailableOfflineToast() {
        Toast.makeText(context, R.string.offline_track_not_available, Toast.LENGTH_SHORT).show();
    }

    private void showUnableToFindTracksToPlayToast() {
        Toast.makeText(context, R.string.playback_missing_playable_tracks, Toast.LENGTH_SHORT).show();
    }

    public boolean showToastOnPlaybackError(Throwable e) {
        if (e instanceof UnskippablePeriodException) {
            showUnskippableAdToast();
            return true;
        } else if (e instanceof TrackNotAvailableOffline) {
            showTrackUnavailableOfflineToast();
            return true;
        } else if (e instanceof IllegalStateException) {
            showUnableToFindTracksToPlayToast();
            return true;
        }
        return false;
    }
}
