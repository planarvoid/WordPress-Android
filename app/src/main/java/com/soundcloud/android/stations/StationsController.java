package com.soundcloud.android.stations;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.propeller.ChangeResult;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;

public class StationsController {
    private final EventBus eventBus;
    private final StationsOperations operations;

    private static final Func1<CurrentPlayQueueTrackEvent, Boolean> isStation = new Func1<CurrentPlayQueueTrackEvent, Boolean>() {
        @Override
        public Boolean call(CurrentPlayQueueTrackEvent event) {
            return event.getCollectionUrn().isStation();
        }
    };

    private static final Func1<PlayQueueEvent, Boolean> isNewPlayQueueAStation = new Func1<PlayQueueEvent, Boolean>() {
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

    @Inject
    public StationsController(EventBus eventBus, StationsOperations operations) {
        this.eventBus = eventBus;
        this.operations = operations;
    }

    public void subscribe() {
        saveCurrentTrackPositionInStation();
        saveRecentlyPlayedStations();
    }

    private void saveCurrentTrackPositionInStation() {
        eventBus
                .queue(EventQueue.PLAY_QUEUE_TRACK)
                .filter(isStation)
                .flatMap(saveLastTrackPosition)
                .subscribe(new DefaultSubscriber<ChangeResult>());
    }

    private void saveRecentlyPlayedStations() {
        eventBus
                .queue(EventQueue.PLAY_QUEUE)
                .filter(isNewPlayQueueAStation)
                .flatMap(saveRecentlyPlayedStation)
                .subscribe(new DefaultSubscriber<ChangeResult>());
    }
}
