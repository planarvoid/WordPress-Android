package com.soundcloud.android.playback.widget;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.rx.PropertySetFunctions;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.internal.util.UtilityFunctions;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayerWidgetController {

    public static final String ACTION_LIKE_CHANGED = "com.soundcloud.android.widgetLike";
    public static final String EXTRA_ADD_LIKE = "isLike";

    private final Context context;
    private final PlayerWidgetPresenter presenter;
    private final PlaySessionStateProvider playSessionsStateProvider;
    private final PlayQueueManager playQueueManager;
    private final TrackOperations trackOperations;
    private final EventBus eventBus;
    private final LikeOperations likeOperations;

    private final Func1<CurrentPlayQueueTrackEvent, Observable<PropertySet>> onPlayQueueEventFunc = new Func1<CurrentPlayQueueTrackEvent, Observable<PropertySet>>() {
        @Override
        public Observable<PropertySet> call(CurrentPlayQueueTrackEvent event) {
            return loadTrackWithAdMeta(event.getCurrentTrackUrn(), event.getCurrentMetaData());
        }
    };

    @Inject
    public PlayerWidgetController(Context context, PlayerWidgetPresenter presenter,
                                  PlaySessionStateProvider playSessionsStateProvider,
                                  PlayQueueManager playQueueManager, TrackOperations trackOperations,
                                  EventBus eventBus, LikeOperations likeOperations) {
        this.context = context;
        this.presenter = presenter;
        this.playSessionsStateProvider = playSessionsStateProvider;
        this.playQueueManager = playQueueManager;
        this.trackOperations = trackOperations;
        this.eventBus = eventBus;
        this.likeOperations = likeOperations;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new TrackChangedSubscriber());
        eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new CurrentUserChangedSubscriber());
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlaybackStateSubscriber());

        eventBus.queue(EventQueue.PLAY_QUEUE_TRACK)
                .flatMap(onPlayQueueEventFunc).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CurrentTrackSubscriber());
    }

    public void update() {
        updatePlayState();
        updatePlayableInformation(UtilityFunctions.<PropertySet>identity());
    }

    private void updatePlayState() {
        presenter.updatePlayState(context, playSessionsStateProvider.isPlaying());
    }

    private void updatePlayableInformation(Func1<PropertySet, PropertySet> updateFunc) {
        Urn currentTrackUrn = playQueueManager.getCurrentTrackUrn();
        if (Urn.NOT_SET.equals(currentTrackUrn)) {
            presenter.reset(context);
        } else {
            loadTrackWithAdMeta(currentTrackUrn, playQueueManager.getCurrentMetaData())
                    .map(updateFunc)
                    .subscribe(new CurrentTrackSubscriber());
        }
    }

    private Observable<PropertySet> loadTrackWithAdMeta(Urn urn, PropertySet metaData) {
        return trackOperations.track(urn).map(PropertySetFunctions.mergeWith(metaData));
    }

    public void handleToggleLikeAction(boolean addLike) {
        final Urn currentTrackUrn = playQueueManager.getCurrentTrackUrn();
        toggleLike(addLike, currentTrackUrn);

        eventBus.publish(EventQueue.TRACKING, UIEvent.fromToggleLike(addLike, Screen.WIDGET.get(), playQueueManager.getScreenTag(), currentTrackUrn));
    }

    private void toggleLike(boolean addLike, Urn trackUrn) {
        final PropertySet propertySet = PropertySet.from(PlayableProperty.URN.bind(trackUrn));
        fireAndForget(addLike
                ? likeOperations.addLike(propertySet)
                : likeOperations.removeLike(propertySet));
    }

    /**
     * Listens for track changes emitted from our application layer via Rx and updates the widget
     * accordingly.
     */
    private final class TrackChangedSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(final EntityStateChangedEvent event) {
            if (playQueueManager.isCurrentTrack(event.getNextUrn())) {
                updatePlayableInformation(PropertySetFunctions.mergeWith(event.getNextChangeSet()));
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

    private class CurrentTrackSubscriber extends DefaultSubscriber<PropertySet> {
        @Override
        public void onNext(PropertySet track) {
            presenter.updateTrackInformation(context, track);
        }
    }

}
