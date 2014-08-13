package com.soundcloud.android.playback.widget;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.functions.Functions;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayerWidgetController {

    public static final String ACTION_LIKE_CHANGED = "com.soundcloud.android.widgetLike";
    public static final String EXTRA_IS_LIKE = "isLike";

    private final Context context;
    private final PlayerWidgetPresenter presenter;
    private final PlaySessionStateProvider playSessionsStateProvider;
    private final PlayQueueManager playQueueManager;
    private final TrackOperations trackOperations;
    private final SoundAssociationOperations soundAssociationOps;
    private final EventBus eventBus;

    @Inject
    public PlayerWidgetController(Context context, PlayerWidgetPresenter presenter,
                                  PlaySessionStateProvider playSessionsStateProvider, PlayQueueManager playQueueManager,
                                  TrackOperations trackOperations, SoundAssociationOperations soundAssocicationOps, EventBus eventBus) {
        this.context = context;
        this.presenter = presenter;
        this.playSessionsStateProvider = playSessionsStateProvider;
        this.playQueueManager = playQueueManager;
        this.trackOperations = trackOperations;
        this.soundAssociationOps = soundAssocicationOps;
        this.eventBus = eventBus;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new PlayableChangedSubscriber());
        eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new CurrentUserChangedSubscriber());
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlaybackStateSubscriber());

        eventBus.queue(EventQueue.PLAY_QUEUE_TRACK).subscribe(new PlayQueueTrackSubscriber());
    }

    public void update() {
        updatePlayState();
        updatePlayableInformation();
    }

    private void updatePlayState() {
        presenter.updatePlayState(context, playSessionsStateProvider.isPlaying());
    }

    private void updatePlayableInformation() {
        updatePlayableInformation(Functions.<PropertySet>identity());
    }

    private void updatePlayableInformation(Func1<PropertySet, PropertySet> updateFunc) {
        TrackUrn currentTrackUrn = playQueueManager.getCurrentTrackUrn();
        if (TrackUrn.NOT_SET.equals(currentTrackUrn)) {
            presenter.reset(context);
        } else {
            trackOperations.track(currentTrackUrn)
                    .map(updateFunc)
                    .subscribe(new CurrentTrackSubscriber());
        }
    }

    // TODO: This method is not specific to the widget, it should be done in a more generic engagements controller
    public void handleToggleLikeAction(boolean isLiked) {
        fireAndForget(trackOperations.track(playQueueManager.getCurrentTrackUrn())
                .flatMap(toggleLike(isLiked))
                .observeOn(AndroidSchedulers.mainThread()));
    }

    private Func1<PropertySet, Observable<PropertySet>> toggleLike(final boolean isLiked) {
        return new Func1<PropertySet, Observable<PropertySet>>() {
            @Override
            public Observable<PropertySet> call(PropertySet track) {
                return soundAssociationOps.toggleLike(track.get(TrackProperty.URN), !isLiked);
            }
        };
    }

    /**
     * Listens for track changes emitted from our application layer via Rx and updates the widget
     * accordingly.
     */
    private final class PlayableChangedSubscriber extends DefaultSubscriber<PlayableUpdatedEvent> {
        @Override
        public void onNext(final PlayableUpdatedEvent event) {
            if (playQueueManager.isCurrentTrack(event.getUrn())) {
                updatePlayableInformation(new Func1<PropertySet, PropertySet>() {
                    @Override
                    public PropertySet call(PropertySet propertySet) {
                        return propertySet.merge(event.getChangeSet());
                    }
                });
            }
        }
    }

    /**
     * When the user logs out, reset all widget instances
     */
    private final class CurrentUserChangedSubscriber extends DefaultSubscriber<CurrentUserChangedEvent> {
        @Override
        public void onNext(CurrentUserChangedEvent event) {
            if (event.getKind() == CurrentUserChangedEvent.USER_REMOVED) {
                presenter.reset(context);
            }
        }
    }

    private class PlaybackStateSubscriber extends DefaultSubscriber<Playa.StateTransition> {
        @Override
        public void onNext(Playa.StateTransition state) {
            presenter.updatePlayState(context, state.playSessionIsActive());
        }
    }

    private class PlayQueueTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent> {
        @Override
        public void onNext(CurrentPlayQueueTrackEvent event) {
            trackOperations.track(event.getCurrentTrackUrn()).subscribe(new CurrentTrackSubscriber());
        }
    }

    private class CurrentTrackSubscriber extends DefaultSubscriber<PropertySet> {
        @Override
        public void onNext(PropertySet track) {
            presenter.updateTrackInformation(context, track);
        }
    }

}
