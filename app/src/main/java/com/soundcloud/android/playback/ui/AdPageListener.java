package com.soundcloud.android.playback.ui;

import com.soundcloud.android.Consts;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.OverlayAdData;
import com.soundcloud.android.ads.PlayableAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.ads.WhyAdsDialogPresenter;
import com.soundcloud.android.deeplinks.DeepLink;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscriber;

import android.content.Context;
import android.net.Uri;

import javax.inject.Inject;

class AdPageListener extends PageListener {

    private final Navigator navigator;
    private final PlayQueueManager playQueueManager;
    private final AdsOperations adsOperations;
    private final WhyAdsDialogPresenter whyAdsPresenter;

    @Inject
    public AdPageListener(Navigator navigator,
                          PlaySessionController playSessionController,
                          PlayQueueManager playQueueManager,
                          EventBus eventBus, AdsOperations adsOperations,
                          WhyAdsDialogPresenter whyAdsPresenter) {
        super(playSessionController, eventBus);
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

    public void onClickThrough(Context activityContext) {
        final Optional<AdData> currentTrackAdData = adsOperations.getCurrentTrackAdData();

        if (currentTrackAdData.isPresent()) {
            adClickThrough(activityContext, (PlayableAdData) currentTrackAdData.get());
        }

        final Optional<AdData> monetizableAdData = adsOperations.getNextTrackAdData();
        if (monetizableAdData.isPresent() && monetizableAdData.get() instanceof OverlayAdData) {
            ((OverlayAdData) monetizableAdData.get()).setMetaAdClicked();
        }
    }

    private void adClickThrough(Context activityContext, PlayableAdData adData) {
        final Uri clickThrough = Uri.parse(adData instanceof AudioAd
                                     ? ((AudioAd) adData).getClickThroughUrl().get()
                                     : ((VideoAd) adData).getClickThroughUrl());
        final DeepLink deepLink = DeepLink.fromUri(clickThrough);

        switch (deepLink) {
            case USER_ENTITY:
            case PLAYLIST_ENTITY:
                openUserOrPlaylistDeeplink(activityContext, deepLink, clickThrough);
                break;
            default:
                navigator.openAdClickthrough(activityContext, clickThrough);
                break;
        }

        eventBus.publish(EventQueue.TRACKING,
                         UIEvent.fromPlayerAdClickThrough(adData, playQueueManager.getCurrentTrackSourceInfo()));
    }

    private void openUserOrPlaylistDeeplink(Context activityContext, DeepLink deeplink, Uri uri) {
        if (playQueueManager.getCurrentPlayQueueItem().isAd()) {
            playQueueManager.moveToNextPlayableItem();
        }

        eventBus.queue(EventQueue.PLAYER_UI)
                .first(PlayerUIEvent.PLAYER_IS_COLLAPSED)
                .subscribe(startPlaylistOrProfile(activityContext, getUrnforEntityDeepLink(deeplink, uri)));

        requestPlayerCollapse();
    }

    private Urn getUrnforEntityDeepLink(DeepLink deepLink, Uri uri) {
        final long id = getIdFromEntityDeepLink(uri);
        if (id != Consts.NOT_SET) {
            switch (deepLink) {
                case PLAYLIST_ENTITY:
                    return Urn.forPlaylist(id);
                case USER_ENTITY:
                    return Urn.forUser(id);
                default:
                    return Urn.NOT_SET;
            }
        }
        return Urn.NOT_SET;
    }

    private long getIdFromEntityDeepLink(Uri uri) {
        try {
            return Long.valueOf(uri.getLastPathSegment());
        } catch (NumberFormatException e) {
            return Consts.NOT_SET;
        }
    }

    public void onAboutAds(Context context) {
        whyAdsPresenter.show(context);
    }

    private Subscriber<PlayerUIEvent> startPlaylistOrProfile(final Context activityContext, final Urn urn) {
        return new DefaultSubscriber<PlayerUIEvent>() {
            @Override
            public void onNext(PlayerUIEvent playerUIEvent) {
                if (urn.isPlaylist()) {
                    final Screen originScreen = Screen.fromTag(playQueueManager.getScreenTag());
                    navigator.legacyOpenPlaylist(activityContext, urn, originScreen);
                } else if (urn.isUser()) {
                    navigator.legacyOpenProfile(activityContext, urn);
                }
            }
        };
    }
}
