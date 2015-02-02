package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.service.Playa;

import android.content.Context;
import android.widget.Toast;

import javax.inject.Inject;

public class PlaybackToastViewController {

    private final Context context;
    private final PlaySessionStateProvider playSessionStateProvider;

    @Inject
    public PlaybackToastViewController(Context context, PlaySessionStateProvider playSessionStateProvider) {
        this.context = context;
        this.playSessionStateProvider = playSessionStateProvider;
    }

    public void showUnskippableAdToast() {
        if (playSessionStateProvider.isPlaying()) {
            Toast.makeText(context, R.string.ad_in_progress, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, R.string.ad_resume_playing_to_continue, Toast.LENGTH_SHORT).show();
        }
    }

    public void showError(Playa.Reason reason){
        if (reason == Playa.Reason.ERROR_FAILED){
            Toast.makeText(context, R.string.playback_error_connection, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, R.string.playback_error_unable_to_play, Toast.LENGTH_SHORT).show();
        }
    }
}
