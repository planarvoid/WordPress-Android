package com.soundcloud.android.analytics.appboy;

import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AdOverlayEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AppboyPlaySessionState {

    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;
    private final AdsOperations adsOperations;

    private boolean sessionPlayed;
    private boolean foreground;
    private boolean adOverlayVisible;
    private boolean expanded;

    @Inject
    public AppboyPlaySessionState(EventBus eventBus,
                                  PlayQueueManager playQueueManager,
                                  AdsOperations adsOperations) {
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
        this.adsOperations = adsOperations;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.PLAYER_UI, new PlayerUiSubscriber());
        eventBus.subscribe(EventQueue.ACTIVITY_LIFE_CYCLE, new ActivityLifecycleSubscriber());
        eventBus.subscribe(EventQueue.AD_OVERLAY, new AdOverlaySubscriber());
    }

    void setSessionPlayed() {
        sessionPlayed = true;
    }

    public void resetSessionPlayed() {
        sessionPlayed = false;
    }

    public boolean isMarketablePlay() {
        return foreground
                && expanded
                && !adOverlayVisible
                && isUserTriggered()
                && !isPromotedTrack()
                && !isAudioAd()
                && !hasAdOverlay();
    }

    boolean isSessionPlayed() {
        return sessionPlayed;
    }

    private boolean isPromotedTrack() {
        PlayQueueItem item = getCurrentPlayQueueItem();
        return item.isTrack() && playQueueManager.isTrackFromCurrentPromotedItem(item.getUrn());
    }

    private boolean isUserTriggered() {
        TrackSourceInfo currentTrackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        return currentTrackSourceInfo != null && currentTrackSourceInfo.getIsUserTriggered();
    }

    private PlayQueueItem getCurrentPlayQueueItem() {
        return playQueueManager.getCurrentPlayQueueItem();
    }

    private boolean isAudioAd() {
        return adsOperations.isCurrentItemAudioAd();
    }

    private boolean hasAdOverlay() {
        return getCurrentPlayQueueItem().getAdData().isPresent();
    }

    private class PlayerUiSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        @Override
        public void onNext(PlayerUIEvent event) {
            if (event.getKind() == PlayerUIEvent.PLAYER_EXPANDED) {
                expanded = true;
            } else if (event.getKind() == PlayerUIEvent.PLAYER_COLLAPSED) {
                expanded = false;
            }
        }
    }

    private class ActivityLifecycleSubscriber extends DefaultSubscriber<ActivityLifeCycleEvent> {
        @Override
        public void onNext(ActivityLifeCycleEvent event) {
            if (event.getKind() == ActivityLifeCycleEvent.ON_RESUME_EVENT) {
                foreground = true;
            } else if (event.getKind() == ActivityLifeCycleEvent.ON_PAUSE_EVENT) {
                foreground = false;
            }
        }
    }

    private class AdOverlaySubscriber extends DefaultSubscriber<AdOverlayEvent> {
        @Override
        public void onNext(AdOverlayEvent event) {
            if (event.getKind() == AdOverlayEvent.SHOWN) {
                adOverlayVisible = true;
            } else if (event.getKind() == AdOverlayEvent.HIDDEN) {
                adOverlayVisible = false;
            }
        }
    }

}
