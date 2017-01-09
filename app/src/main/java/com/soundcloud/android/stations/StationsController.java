package com.soundcloud.android.stations;

import static com.soundcloud.android.events.UrnStateChangedEvent.fromStationsUpdated;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
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

    private static final Func2<CurrentPlayQueueItemEvent, PlayStateEvent, CollectionPlaybackState> TO_COLLECTION_PLAY_STATE = (event, playStateEvent) -> new CollectionPlaybackState(
            event.getCollectionUrn(),
            event.getPosition(),
            playStateEvent.getNewState()
    );

    private final Action1<CollectionPlaybackState> saveRecentStation = new Action1<CollectionPlaybackState>() {
        @Override
        public void call(CollectionPlaybackState collectionPlaybackState) {
            operations.saveLastPlayedTrackPosition(collectionPlaybackState.collectionUrn,
                                                   collectionPlaybackState.position);
            operations.saveRecentlyPlayedStation(collectionPlaybackState.collectionUrn);
            eventBus.publish(EventQueue.URN_STATE_CHANGED,
                             fromStationsUpdated(collectionPlaybackState.collectionUrn));
        }
    };

    private static final Func1<CollectionPlaybackState, Boolean> IS_PLAYING_STATION = collectionPlaybackState -> collectionPlaybackState.collectionUrn.isStation() && collectionPlaybackState.playbackState.isPlayerPlaying();

    @Inject
    public StationsController(EventBus eventBus,
                              StationsOperations operations,
                              @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.eventBus = eventBus;
        this.operations = operations;
        this.scheduler = scheduler;
    }

    public void subscribe() {
        Observable
                .combineLatest(
                        eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM),
                        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED),
                        TO_COLLECTION_PLAY_STATE
                )
                .filter(IS_PLAYING_STATION)
                .observeOn(scheduler)
                .doOnNext(saveRecentStation)
                .subscribe(new DefaultSubscriber<CollectionPlaybackState>());
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
