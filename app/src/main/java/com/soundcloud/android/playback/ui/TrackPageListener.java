package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.playqueue.PlayQueueUIEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscriber;

import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Inject;


class TrackPageListener extends PageListener {
    private final PlayQueueManager playQueueManager;
    private final LikeOperations likeOperations;
    private final NavigationExecutor navigationExecutor;
    private final EngagementsTracking engagementsTracking;

    @Inject
    public TrackPageListener(PlaySessionController playSessionController,
                             PlayQueueManager playQueueManager,
                             EventBus eventBus, LikeOperations likeOperations, NavigationExecutor navigationExecutor,
                             EngagementsTracking engagementsTracking) {
        super(playSessionController, eventBus);
        this.playQueueManager = playQueueManager;
        this.likeOperations = likeOperations;
        this.navigationExecutor = navigationExecutor;
        this.engagementsTracking = engagementsTracking;
    }

    public void onToggleLike(final boolean addLike, @NonNull final Urn trackUrn) {
        checkNotNull(trackUrn);
        likeOperations.toggleLikeAndForget(trackUrn, addLike);

        engagementsTracking.likeTrackUrn(trackUrn,
                                         addLike,
                                         getEventContextMetadata(trackUrn),
                                         playQueueManager.getCurrentPromotedSourceInfo(trackUrn));
    }

    public void onUpsell(final Context activityContext, final Urn trackUrn) {
        navigationExecutor.openUpgrade(activityContext, UpsellContext.PREMIUM_CONTENT);
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlayerClick(trackUrn));
    }

    private EventContextMetadata getEventContextMetadata(Urn trackUrn) {
        return EventContextMetadata.builder()
                                   .pageName(Screen.PLAYER_MAIN.get())
                                   .trackSourceInfo(playQueueManager.getCurrentTrackSourceInfo())
                                   .pageUrn(trackUrn)
                                   .build();
    }

    public void onGotoUser(final Context activityContext, final Urn userUrn) {
        eventBus.queue(EventQueue.PLAYER_UI)
                .first(PlayerUIEvent.PLAYER_IS_COLLAPSED)
                .subscribe(startProfileActivity(activityContext, userUrn));

        requestPlayerCollapse();
    }

    void onPlayQueue() {
        eventBus.publish(EventQueue.PLAY_QUEUE_UI, PlayQueueUIEvent.createDisplayEvent());
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueOpen());
    }

    private Subscriber<PlayerUIEvent> startProfileActivity(final Context activityContext, final Urn userUrn) {
        return new DefaultSubscriber<PlayerUIEvent>() {
            @Override
            public void onNext(PlayerUIEvent playerUIEvent) {
                navigationExecutor.legacyOpenProfile(activityContext, userUrn);
            }
        };
    }
}
