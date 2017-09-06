package com.soundcloud.android.stations;

import static com.soundcloud.android.events.UrnStateChangedEvent.fromStationsUpdated;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.rx.observers.DefaultCompletableObserver;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Observable;
import io.reactivex.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;

public class StationsController {
    private final EventBusV2 eventBus;
    private final StationsOperations operations;
    private final SyncInitiator syncInitiator;
    private final Scheduler scheduler;

    @Inject
    StationsController(EventBusV2 eventBus,
                       StationsOperations operations,
                       SyncInitiator syncInitiator,
                       @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.eventBus = eventBus;
        this.operations = operations;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
    }

    public void subscribe() {
        Observable
                .combineLatest(
                        eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM),
                        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED),
                        (event, playStateEvent) -> new CollectionPlaybackState(
                                event.getCollectionUrn(),
                                event.getPosition(),
                                playStateEvent.getNewState()
                        )
                )
                .filter(collectionPlaybackState -> collectionPlaybackState.collectionUrn.isStation() && collectionPlaybackState.playbackState.isPlayerPlaying())
                .observeOn(scheduler)
                .doOnNext(this::saveStation)
                .flatMapCompletable(__ -> syncInitiator.requestSystemSync())
                .subscribe(new DefaultCompletableObserver());
    }

    private void saveStation(CollectionPlaybackState collectionPlaybackState) {
        // operations should not be imperative. These should be observables
        operations.saveLastPlayedTrackPosition(collectionPlaybackState.collectionUrn, collectionPlaybackState.position);
        operations.saveRecentlyPlayedStation(collectionPlaybackState.collectionUrn);
        eventBus.publish(EventQueue.URN_STATE_CHANGED, fromStationsUpdated(collectionPlaybackState.collectionUrn));
    }

    private static class CollectionPlaybackState {
        private final Urn collectionUrn;
        private final int position;
        private final PlaybackState playbackState;

        CollectionPlaybackState(Urn collectionUrn, int position, PlaybackState playbackState) {
            this.collectionUrn = collectionUrn;
            this.position = position;
            this.playbackState = playbackState;
        }
    }

}
