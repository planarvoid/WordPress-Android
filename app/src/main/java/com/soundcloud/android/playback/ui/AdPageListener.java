package com.soundcloud.android.playback.ui;

import static eu.inmite.android.lib.dialogs.SimpleDialogFragment.createBuilder;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.propeller.PropertySet;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;

import javax.inject.Inject;

class AdPageListener extends PageListener {

    private final Context context;
    private final PlayQueueManager playQueueManager;

    @Inject
    public AdPageListener(Context context,
                          PlaySessionStateProvider playSessionStateProvider,
                          PlaybackOperations playbackOperations,
                          PlayQueueManager playQueueManager,
                          EventBus eventBus) {
        super(playbackOperations, playSessionStateProvider, eventBus);
        this.context = context;
        this.playQueueManager = playQueueManager;
    }

    public void onNext() {
        playbackOperations.nextTrack();
    }

    public void onPrevious() {
        playbackOperations.previousTrack();
    }

    public void skipAd() {
        playbackOperations.nextTrack();
    }

    public void onClickThrough() {
        final PropertySet audioAd = playQueueManager.getAudioAd();
        Uri uri = audioAd.get(AdProperty.CLICK_THROUGH_LINK);
        startActivity(uri);

        // track this click
        eventBus.publish(EventQueue.UI, UIEvent.fromAudioAdClick(audioAd, playQueueManager.getCurrentTrackUrn()));
    }

    public void onAboutAds(FragmentActivity activity) {
        createBuilder(activity, activity.getSupportFragmentManager())
                .setTitle(R.string.why_ads)
                .setMessage(R.string.why_ads_dialog_message)
                .setPositiveButtonText(android.R.string.ok)
                .show();
    }

    private void startActivity(Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

}
