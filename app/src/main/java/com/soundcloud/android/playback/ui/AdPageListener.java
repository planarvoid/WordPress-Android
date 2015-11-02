package com.soundcloud.android.playback.ui;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.OverlayAdData;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.java.optional.Optional;
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
                          PlaySessionController playSessionController,
                          PlayQueueManager playQueueManager,
                          EventBus eventBus, AdsOperations adsOperations,
                          AccountOperations accountOperations,
                          WhyAdsDialogPresenter whyAdsPresenter) {
        super(playSessionController, playSessionStateProvider, eventBus);
        this.context = context;
        this.playQueueManager = playQueueManager;
        this.adsOperations = adsOperations;
        this.accountOperations = accountOperations;
        this.whyAdsPresenter = whyAdsPresenter;
    }

    public void onNext() {
        playSessionController.nextTrack();
        eventBus.publish(EventQueue.TRACKING, PlayControlEvent.skip(PlayControlEvent.SOURCE_FULL_PLAYER));
    }

    public void onPrevious() {
        playSessionController.previousTrack();
        eventBus.publish(EventQueue.TRACKING, PlayControlEvent.previous(PlayControlEvent.SOURCE_FULL_PLAYER));
    }

    public void onSkipAd() {
        playSessionController.nextTrack();
        eventBus.publish(EventQueue.TRACKING, PlayControlEvent.skipAd());
    }

    public void onClickThrough() {
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        final AudioAd audioAdData = (AudioAd) currentPlayQueueItem.getAdData().get();
        final Optional<AdData> monetizableAdData = adsOperations.getMonetizableTrackAdData();
        final Urn trackUrn = currentPlayQueueItem.getUrn();

        startActivity(audioAdData.getVisualAd().getClickThroughUrl());

        if (monetizableAdData.isPresent() && monetizableAdData.get() instanceof OverlayAdData) {
            ((OverlayAdData) monetizableAdData.get()).setMetaAdClicked();
        }

        eventBus.publish(EventQueue.TRACKING, UIEvent.fromAudioAdClick(audioAdData, trackUrn, accountOperations.getLoggedInUserUrn(), playQueueManager.getCurrentTrackSourceInfo()));
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
