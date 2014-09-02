package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.users.UserUrn;
import rx.functions.Action1;
import rx.functions.Func1;

import android.content.Context;

import javax.inject.Inject;

class TrackPageListener {
    private static final Func1<PlayerUIEvent, Boolean> PLAYER_IS_COLLAPASED = new Func1<PlayerUIEvent, Boolean>() {
        @Override
        public Boolean call(PlayerUIEvent playerUIEvent) {
            return playerUIEvent.getKind() == PlayerUIEvent.PLAYER_COLLAPSED;
        }
    };

    private final PlaybackOperations playbackOperations;
    private final SoundAssociationOperations associationOperations;
    private final PlayQueueManager playQueueManager;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final EventBus eventBus;

    @Inject
    public TrackPageListener(PlaybackOperations playbackOperations,
                             SoundAssociationOperations associationOperations,
                             PlayQueueManager playQueueManager,
                             PlaySessionStateProvider playSessionStateProvider,
                             EventBus eventBus) {
        this.playbackOperations = playbackOperations;
        this.associationOperations = associationOperations;
        this.playQueueManager = playQueueManager;
        this.playSessionStateProvider = playSessionStateProvider;
        this.eventBus = eventBus;
    }

    public void onTogglePlay() {
        playbackOperations.togglePlayback();
        trackTogglePlay(PlayControlEvent.SOURCE_FULL_PLAYER);
    }

    public void onFooterTap() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
    }

    public void onPlayerClose() {
        requestPlayerCollapse();
    }

    public void onToggleLike(boolean isLike) {
        fireAndForget(associationOperations.toggleLike(playQueueManager.getCurrentTrackUrn(), isLike));
    }

    public void onGotoUser(final Context activityContext, final UserUrn userUrn) {
        eventBus.queue(EventQueue.PLAYER_UI)
                .first(PLAYER_IS_COLLAPASED)
                .subscribe(startProfileActivity(activityContext, userUrn));

        requestPlayerCollapse();
    }

    public void onFooterTogglePlay() {
        playbackOperations.togglePlayback();
        trackTogglePlay(PlayControlEvent.SOURCE_FOOTER_PLAYER);
    }

    private void trackTogglePlay(String location) {
        if (playSessionStateProvider.isPlaying()) {
            eventBus.publish(EventQueue.PLAY_CONTROL, PlayControlEvent.pause(location));
        } else {
            eventBus.publish(EventQueue.PLAY_CONTROL, PlayControlEvent.play(location));
        }
    }

    private void requestPlayerCollapse() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayer());
    }

    private Action1<PlayerUIEvent> startProfileActivity(final Context activityContext, final UserUrn userUrn) {
        return new Action1<PlayerUIEvent>() {
            @Override
            public void call(PlayerUIEvent playerUIEvent) {
                activityContext.startActivity(ProfileActivity.getIntent(activityContext, userUrn));
            }
        };
    }
}
