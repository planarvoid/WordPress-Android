package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.rx.eventbus.EventBus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import javax.inject.Inject;

class AdPageListener {

    private final Context context;
    private final PlaybackOperations playbackOperations;
    private final PlayQueueManager playQueueManager;
    private final EventBus eventBus;

    @Inject
    public AdPageListener(Context context,
                          PlaybackOperations playbackOperations,
                          PlayQueueManager playQueueManager,
                          EventBus eventBus) {
        this.context = context;
        this.playbackOperations = playbackOperations;
        this.playQueueManager = playQueueManager;
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
        if (playQueueManager.isCurrentTrackAudioAd()) {
            Uri uri = playQueueManager.getAudioAd().get(AdProperty.CLICK_THROUGH_LINK);
            startActivity(uri);
        }
    }

    public void onAboutAds(final Context context) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.why_ads))
                .setMessage(context.getString(R.string.why_ads_dialog_message))
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    public void onNext() {
        playbackOperations.nextTrack();
    }

    public void onPrevious() {
        playbackOperations.previousTrack();
    }

    private void startActivity(Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
