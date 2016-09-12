package com.soundcloud.android.playback.ui;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.OverlayAdData;
import com.soundcloud.android.ads.PlayerAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;

import javax.inject.Inject;

class AdPageListener extends PageListener {

    private final Context context;
    private final Navigator navigator;
    private final PlayQueueManager playQueueManager;
    private final AdsOperations adsOperations;
    private final WhyAdsDialogPresenter whyAdsPresenter;

    @Inject
    public AdPageListener(Context context,
                          Navigator navigator,
                          PlaySessionController playSessionController,
                          PlayQueueManager playQueueManager,
                          EventBus eventBus, AdsOperations adsOperations,
                          WhyAdsDialogPresenter whyAdsPresenter) {
        super(playSessionController, eventBus);
        this.context = context;
        this.navigator = navigator;
        this.playQueueManager = playQueueManager;
        this.adsOperations = adsOperations;
        this.whyAdsPresenter = whyAdsPresenter;
    }

    public void onNext() {
        playSessionController.nextTrack();
    }

    public void onPrevious() {
        playSessionController.previousTrack();
    }

    public void onSkipAd() {
        playSessionController.nextTrack();
    }

    public void onFullscreen() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.forcePlayerLandscape());
    }

    public void onShrink() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.forcePlayerPortrait());
    }

    public void onClickThrough() {
        adClickThrough((PlayerAdData) adsOperations.getCurrentTrackAdData().get());

        final Optional<AdData> monetizableAdData = adsOperations.getNextTrackAdData();
        if (monetizableAdData.isPresent() && monetizableAdData.get() instanceof OverlayAdData) {
            ((OverlayAdData) monetizableAdData.get()).setMetaAdClicked();
        }
    }

    private void adClickThrough(PlayerAdData adData) {
        if (adData instanceof AudioAd) {
            navigator.openAdClickthrough(context, ((AudioAd) adData).getClickThroughUrl().get());
        } else {
            navigator.openAdClickthrough(context, ((VideoAd) adData).getClickThroughUrl());
        }
        eventBus.publish(EventQueue.TRACKING,
                         UIEvent.fromAdClickThrough(adData, playQueueManager.getCurrentTrackSourceInfo()));
    }

    public void onAboutAds(Context context) {
        whyAdsPresenter.show(context);
    }

}
