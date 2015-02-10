package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaySessionStateProvider;

import android.content.Context;
import android.widget.Toast;

import javax.inject.Inject;

public class AdToastViewController {

    private final Context context;
    private final PlaySessionStateProvider playSessionStateProvider;

    @Inject
    public AdToastViewController(Context context, PlaySessionStateProvider playSessionStateProvider) {
        this.context = context;
        this.playSessionStateProvider = playSessionStateProvider;
    }

    public void showUnskippableAdToast() {
        Toast.makeText(context, playSessionStateProvider.isPlaying()
                ? R.string.ad_in_progress
                : R.string.ad_resume_playing_to_continue,
                Toast.LENGTH_SHORT).show();
    }

}
