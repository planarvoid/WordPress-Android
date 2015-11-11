package com.soundcloud.android.playback.widget;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueFunctions;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.PropertySetFunctions;
import com.soundcloud.rx.eventbus.EventBus;
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
    private final TrackRepository trackRepository;
    private final EventBus eventBus;
    private final LikeOperations likeOperations;
    private final EngagementsTracking engagementsTracking;

    private final Func1<TrackQueueItem, Observable<PropertySet>> onPlayQueueEventFunc = new Func1<TrackQueueItem, Observable<PropertySet>>() {
        @Override
        public Observable<PropertySet> call(TrackQueueItem playQueueItem) {
            return loadTrackWithAdMeta(playQueueItem);
        }
    };

    @Inject
    public PlayerWidgetController(Context context, PlayerWidgetPresenter presenter,
                                  PlaySessionStateProvider playSessionsStateProvider,
                                  PlayQueueManager playQueueManager, TrackRepository trackRepository,
                                  EventBus eventBus, LikeOperations likeOperations,
                                  EngagementsTracking engagementsTracking) {
        this.context = context;
        this.presenter = presenter;
        this.playSessionsStateProvider = playSessionsStateProvider;
        this.playQueueManager = playQueueManager;
        this.trackRepository = trackRepository;
        this.eventBus = eventBus;
        this.likeOperations = likeOperations;
        this.engagementsTracking = engagementsTracking;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new TrackChangedSubscriber());
        eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new CurrentUserChangedSubscriber());
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlaybackStateSubscriber());
        eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                .filter(PlayQueueFunctions.IS_TRACK_QUEUE_ITEM)
                .map(PlayQueueFunctions.TO_TRACK_QUEUE_ITEM)
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
        if (playQueueManager.getCurrentPlayQueueItem().isTrack()) {
            loadTrackWithAdMeta((TrackQueueItem) playQueueManager.getCurrentPlayQueueItem())
                    .map(updateFunc)
                    .subscribe(new CurrentTrackSubscriber());
        } else {
            presenter.reset(context);
        }
    }

    private Observable<PropertySet> loadTrackWithAdMeta(TrackQueueItem currentTrackQueueItem) {
            return trackRepository.track(currentTrackQueueItem.getTrackUrn())
                    .map(PropertySetFunctions.mergeWith(currentTrackQueueItem.getMetaData()));
    }

    public void handleToggleLikeAction(final boolean addLike) {
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        if (currentPlayQueueItem.isTrack()) {
            final Urn currentTrackUrn = currentPlayQueueItem.getUrn();
            fireAndForget(likeOperations.toggleLike(currentTrackUrn, addLike));

            engagementsTracking.likeTrackUrn(currentTrackUrn,
                    addLike,
                    getEventMetadata(),
                    playQueueManager.getCurrentPromotedSourceInfo(currentTrackUrn));
        } else {
            ErrorUtils.handleSilentException(new IllegalStateException("Tried to like a track from widget with invalid playQueue item"));
        }
    }

    private EventContextMetadata getEventMetadata() {
        return EventContextMetadata.builder()
                .invokerScreen(Screen.WIDGET.get())
                .contextScreen(playQueueManager.getScreenTag())
                .pageName(Screen.WIDGET.get())
                .pageUrn(Urn.NOT_SET)
                .build();
    }

    /**
     * Listens for track changes emitted from our application layer via Rx and updates the widget
     * accordingly.
     */
    private final class TrackChangedSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(final EntityStateChangedEvent event) {
            if (!playQueueManager.isQueueEmpty() && playQueueManager.isCurrentTrack(event.getFirstUrn())) {
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

    private class PlaybackStateSubscriber extends DefaultSubscriber<Player.StateTransition> {
        @Override
        public void onNext(Player.StateTransition state) {
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
