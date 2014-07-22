package com.soundcloud.android.playback.ui;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.rx.eventbus.EventBus;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import javax.inject.Inject;

class AdPageListener {

    private final Context context;
    private final PlaybackOperations playbackOperations;
    private final EventBus eventBus;

    @Inject
    public AdPageListener(Context context,
                          PlaybackOperations playbackOperations,
                          EventBus eventBus) {
        this.context = context;
        this.playbackOperations = playbackOperations;
        this.eventBus = eventBus;
    }

    public void onTogglePlay() {
        playbackOperations.togglePlayback();
    }

    public void skipAd() {
        playbackOperations.nextTrack();
    }

    public void onFooterTap() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forExpandPlayer());
    }

    public void onPlayerClose() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forCollapsePlayer());
    }

    public void onClickThrough()  {
        // To be implemented
    }

    public void onAboutAds() {
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://soundcloud.com"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
