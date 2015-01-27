package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.propeller.PropertySet;
import rx.Subscriber;

import android.content.Context;

import javax.inject.Inject;

class TrackPageListener extends PageListener {
    private final SoundAssociationOperations associationOperations;
    private final PlayQueueManager playQueueManager;
    private final FeatureFlags featureFlags;
    private final LikeOperations likeOperations;

    @Inject
    public TrackPageListener(PlaybackOperations playbackOperations,
                             SoundAssociationOperations associationOperations,
                             PlayQueueManager playQueueManager,
                             PlaySessionStateProvider playSessionStateProvider,
                             EventBus eventBus, FeatureFlags featureFlags, LikeOperations likeOperations) {
        super(playbackOperations, playSessionStateProvider, eventBus);
        this.associationOperations = associationOperations;
        this.playQueueManager = playQueueManager;
        this.featureFlags = featureFlags;
        this.likeOperations = likeOperations;
    }

    public void onToggleLike(boolean addLike, Urn trackUrn) {
        if (featureFlags.isEnabled(Flag.NEW_LIKES_END_TO_END)) {
            PropertySet propertySet = PropertySet.from(PlayableProperty.URN.bind(trackUrn));
            fireAndForget(addLike
                    ? likeOperations.addLike(propertySet)
                    : likeOperations.removeLike(propertySet));
        } else {
            fireAndForget(associationOperations.toggleLike(trackUrn, addLike));
        }
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromToggleLike(addLike, ScreenElement.PLAYER.get(), playQueueManager.getScreenTag(), trackUrn));
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
                activityContext.startActivity(ProfileActivity.getIntent(activityContext, userUrn));
            }
        };
    }
}
