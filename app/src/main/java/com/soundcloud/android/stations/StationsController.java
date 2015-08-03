package com.soundcloud.android.stations;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.rx.eventbus.EventBus;
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

    private final Func1<CurrentPlayQueueTrackEvent, Observable<ChangeResult>> toChangeResult = new Func1<CurrentPlayQueueTrackEvent, Observable<ChangeResult>>() {
        @Override
        public Observable<ChangeResult> call(CurrentPlayQueueTrackEvent event) {
            return operations.saveLastPlayedTrackPosition(event.getCollectionUrn(), event.getPosition());
        }
    };

    @Inject
    public StationsController(EventBus eventBus, StationsOperations operations) {
        this.eventBus = eventBus;
        this.operations = operations;
    }

    public void subscribe() {
        fireAndForget(eventBus
                .queue(EventQueue.PLAY_QUEUE_TRACK)
                .filter(isStation)
                .flatMap(toChangeResult)
        );
    }
}
