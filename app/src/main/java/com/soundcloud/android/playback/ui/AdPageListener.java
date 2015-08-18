package com.soundcloud.android.playback.ui;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.LeaveBehindProperty;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import javax.inject.Inject;

class AdPageListener extends PageListener {

    private final Context context;
    private final PlayQueueManager playQueueManager;
    private final AdsOperations adsOperations;
    private final AccountOperations accountOperations;
    private final WhyAdsDialogPresenter whyAdsPresenter;

    @Inject
    public AdPageListener(Context context,
                          PlaySessionStateProvider playSessionStateProvider,
                          PlaybackOperations playbackOperations,
                          PlayQueueManager playQueueManager,
                          EventBus eventBus, AdsOperations adsOperations,
                          AccountOperations accountOperations,
                          WhyAdsDialogPresenter whyAdsPresenter) {
        super(playbackOperations, playSessionStateProvider, eventBus);
        this.context = context;
        this.playQueueManager = playQueueManager;
        this.adsOperations = adsOperations;
        this.accountOperations = accountOperations;
        this.whyAdsPresenter = whyAdsPresenter;
    }

    public void onNext() {
        playbackOperations.nextTrack();
        eventBus.publish(EventQueue.TRACKING, PlayControlEvent.skip(PlayControlEvent.SOURCE_FULL_PLAYER));
    }

    public void onPrevious() {
        playbackOperations.previousTrack();
        eventBus.publish(EventQueue.TRACKING, PlayControlEvent.previous(PlayControlEvent.SOURCE_FULL_PLAYER));
    }

    public void onSkipAd() {
        playbackOperations.nextTrack();
        eventBus.publish(EventQueue.TRACKING, PlayControlEvent.skipAd());
    }

    public void onClickThrough() {
        final PropertySet audioAd = playQueueManager.getCurrentMetaData();
        Uri uri = audioAd.get(AdProperty.CLICK_THROUGH_LINK);
        startActivity(uri);

        adsOperations.getMonetizableTrackMetaData().put(LeaveBehindProperty.META_AD_CLICKED, true);
        // track this click
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromAudioAdClick(audioAd, playQueueManager.getCurrentTrackUrn(), accountOperations.getLoggedInUserUrn(), playQueueManager.getCurrentTrackSourceInfo()));
    }

    public void onAboutAds(Context context) {
        whyAdsPresenter.show(context);
    }

    private void startActivity(Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

}
