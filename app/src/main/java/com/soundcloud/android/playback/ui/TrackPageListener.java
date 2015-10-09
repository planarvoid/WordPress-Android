package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlayableMetadata;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscriber;
import rx.functions.Func1;

import android.content.Context;

import javax.inject.Inject;


class TrackPageListener extends PageListener {
    private final PlayQueueManager playQueueManager;
    private final TrackRepository trackRepository;
    private final LikeOperations likeOperations;
    private final Navigator navigator;

    @Inject
    public TrackPageListener(PlaySessionController playSessionController,
                             PlayQueueManager playQueueManager,
                             PlaySessionStateProvider playSessionStateProvider,
                             TrackRepository trackRepository,
                             EventBus eventBus, LikeOperations likeOperations, Navigator navigator) {
        super(playSessionController, playSessionStateProvider, eventBus);
        this.playQueueManager = playQueueManager;
        this.trackRepository = trackRepository;
        this.likeOperations = likeOperations;
        this.navigator = navigator;
    }

    public void onToggleLike(final boolean addLike, final Urn trackUrn) {
        final PromotedSourceInfo promotedSourceInfo = playQueueManager.getCurrentPromotedSourceInfo(trackUrn);

        fireAndForget(likeOperations.toggleLike(trackUrn, addLike));

        trackRepository.track(trackUrn)
                .map(likeEventFromTrack(addLike, trackUrn, promotedSourceInfo))
                .subscribe(eventBus.queue(EventQueue.TRACKING));
    }

    public void onGotoUser(final Context activityContext, final Urn userUrn) {
        eventBus.queue(EventQueue.PLAYER_UI)
                .first(PlayerUIEvent.PLAYER_IS_COLLAPSED)
                .subscribe(startProfileActivity(activityContext, userUrn));

        requestPlayerCollapse();
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayerClose(UIEvent.METHOD_PROFILE_OPEN));
    }

    public void onScrub(int newScrubState) {
        if (newScrubState == ScrubController.SCRUB_STATE_SCRUBBING) {
            eventBus.publish(EventQueue.TRACKING, PlayControlEvent.scrub(PlayControlEvent.SOURCE_FULL_PLAYER));
        }
    }

    private Subscriber<PlayerUIEvent> startProfileActivity(final Context activityContext, final Urn userUrn) {
        return new DefaultSubscriber<PlayerUIEvent>() {
            @Override
            public void onNext(PlayerUIEvent playerUIEvent) {
                navigator.openProfile(activityContext, userUrn);
            }
        };
    }

    private Func1<PropertySet, UIEvent> likeEventFromTrack(final boolean addLike, final Urn trackUrn, final PromotedSourceInfo promotedSourceInfo) {
        return new Func1<PropertySet, UIEvent>() {
            @Override
            public UIEvent call(PropertySet track) {
                return UIEvent.fromToggleLike(addLike,
                        ScreenElement.PLAYER.get(),
                        playQueueManager.getScreenTag(),
                        Screen.PLAYER_MAIN.get(),
                        trackUrn,
                        trackUrn,
                        promotedSourceInfo,
                        PlayableMetadata.from(track));
            }
        };
    }
}
