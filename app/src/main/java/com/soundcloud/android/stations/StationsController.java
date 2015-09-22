package com.soundcloud.android.stations;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;

public class StationsController {
    private final EventBus eventBus;
    private final StationsOperations operations;

    private static final Func1<CurrentPlayQueueTrackEvent, Boolean> IS_STATION = new Func1<CurrentPlayQueueTrackEvent, Boolean>() {
        @Override
        public Boolean call(CurrentPlayQueueTrackEvent event) {
            return event.getCollectionUrn().isStation();
        }
    };

    private static final Func1<PlayQueueEvent, Boolean> IS_NEW_PLAY_QUEUE_A_STATION = new Func1<PlayQueueEvent, Boolean>() {
        @Override
        public Boolean call(PlayQueueEvent playQueueEvent) {
            return playQueueEvent.isNewQueue() && playQueueEvent.getCollectionUrn().isStation();
        }
    };

    private final Func1<CurrentPlayQueueTrackEvent, Observable<ChangeResult>> saveLastTrackPosition = new Func1<CurrentPlayQueueTrackEvent, Observable<ChangeResult>>() {
        @Override
        public Observable<ChangeResult> call(CurrentPlayQueueTrackEvent event) {
            return operations.saveLastPlayedTrackPosition(event.getCollectionUrn(), event.getPosition());
        }
    };

    private final Func1<PlayQueueEvent, Observable<ChangeResult>> saveRecentlyPlayedStation = new Func1<PlayQueueEvent, Observable<ChangeResult>>() {
        @Override
        public Observable<ChangeResult> call(PlayQueueEvent event) {
            return operations.saveRecentlyPlayedStation(event.getCollectionUrn());
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

    @Inject
    public StationsController(EventBus eventBus, StationsOperations operations) {
        this.eventBus = eventBus;
        this.operations = operations;
    }

    public void subscribe() {
        syncStationsUponLogin();
        saveCurrentTrackPositionInStation();
        saveRecentlyPlayedStations();
    }

    private void syncStationsUponLogin() {
        eventBus.queue(EventQueue.CURRENT_USER_CHANGED)
                .filter(IS_LOGGED_IN)
                .flatMap(syncStations)
                .subscribe(new DefaultSubscriber<SyncResult>());
    }

    private void saveCurrentTrackPositionInStation() {
        eventBus.queue(EventQueue.PLAY_QUEUE_TRACK)
                .filter(IS_STATION)
                .flatMap(saveLastTrackPosition)
                .subscribe(new DefaultSubscriber<ChangeResult>());
    }

    private void saveRecentlyPlayedStations() {
        eventBus.queue(EventQueue.PLAY_QUEUE)
                .filter(IS_NEW_PLAY_QUEUE_A_STATION)
                .flatMap(saveRecentlyPlayedStation)
                .subscribe(new DefaultSubscriber<ChangeResult>());
    }
}
