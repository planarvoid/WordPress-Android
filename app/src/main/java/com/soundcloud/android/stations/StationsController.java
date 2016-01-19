package com.soundcloud.android.stations;

import static com.soundcloud.android.events.EntityStateChangedEvent.fromStationsUpdated;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;

public class StationsController {
    private final EventBus eventBus;
    private final StationsOperations operations;
    private final Scheduler scheduler;

    public static final Func2<CurrentPlayQueueItemEvent, Player.StateTransition, CollectionPlayState> TO_COLLECTION_PLAY_STATE = new Func2<CurrentPlayQueueItemEvent, Player.StateTransition, CollectionPlayState>() {
        @Override
        public StationsController.CollectionPlayState call(CurrentPlayQueueItemEvent event, Player.StateTransition stateTransition) {
            return new StationsController.CollectionPlayState(
                    event.getCollectionUrn(),
                    event.getPosition(),
                    stateTransition.getNewState()
            );
        }
    };
    
    private static final Func1<CurrentUserChangedEvent, Boolean> IS_LOGGED_IN = new Func1<CurrentUserChangedEvent, Boolean>() {
        @Override
        public Boolean call(CurrentUserChangedEvent currentUserChangedEvent) {
            return currentUserChangedEvent.getKind() == CurrentUserChangedEvent.USER_UPDATED;
        }
    };

    private final Func1<CurrentUserChangedEvent, Observable<SyncResult>> syncStations = new Func1<CurrentUserChangedEvent, Observable<SyncResult>>() {
        @Override
        public Observable<SyncResult> call(CurrentUserChangedEvent currentUserChangedEvent) {
            return operations.sync();
        }
    };

    private final Action1<CollectionPlayState> saveRecentStation = new Action1<CollectionPlayState>() {
        @Override
        public void call(CollectionPlayState collectionPlayState) {
            operations.saveLastPlayedTrackPosition(collectionPlayState.collectionUrn, collectionPlayState.position);
            operations.saveRecentlyPlayedStation(collectionPlayState.collectionUrn);
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, fromStationsUpdated(collectionPlayState.collectionUrn));
        }
    };

    private static final Func1<CollectionPlayState, Boolean> IS_PLAYING_STATION = new Func1<CollectionPlayState, Boolean>() {
        @Override
        public Boolean call(CollectionPlayState collectionPlayState) {
            return collectionPlayState.collectionUrn.isStation() && collectionPlayState.playerState.isPlayerPlaying();
        }
    };

    @Inject
    public StationsController(EventBus eventBus,
                              StationsOperations operations,
                              @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.eventBus = eventBus;
        this.operations = operations;
        this.scheduler = scheduler;
    }

    public void subscribe() {
        syncStationsUponLogin();
        saveRecentStation();
    }

    private void syncStationsUponLogin() {
        eventBus.queue(EventQueue.CURRENT_USER_CHANGED)
                .filter(IS_LOGGED_IN)
                .flatMap(syncStations)
                .subscribe(new DefaultSubscriber<SyncResult>());
    }

    private void saveRecentStation() {
        Observable
                .combineLatest(
                        eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM),
                        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED),
                        TO_COLLECTION_PLAY_STATE
                )
                .filter(IS_PLAYING_STATION)
                .observeOn(scheduler)
                .doOnNext(saveRecentStation)
                .subscribe(new DefaultSubscriber<CollectionPlayState>());
    }

    private static class CollectionPlayState {
        private final Urn collectionUrn;
        private final int position;
        private final Player.PlayerState playerState;

        public CollectionPlayState(Urn collectionUrn, int position, Player.PlayerState playerState) {
            this.collectionUrn = collectionUrn;
            this.position = position;
            this.playerState = playerState;
        }
    }

}
