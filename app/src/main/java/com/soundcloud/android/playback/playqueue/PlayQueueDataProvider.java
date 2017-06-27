package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Observable;
import io.reactivex.Single;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.List;

public class PlayQueueDataProvider {

    private final PlayQueueOperations playQueueOperations;
    private final PlayQueueUIItemMapper playQueueUIItemMapper;
    private final EventBusV2 eventBus;

    @Inject
    PlayQueueDataProvider(PlayQueueOperations playQueueOperations, PlayQueueUIItemMapper playQueueUIItemMapper, EventBusV2 eventBus) {
        this.playQueueOperations = playQueueOperations;
        this.playQueueUIItemMapper = playQueueUIItemMapper;
        this.eventBus = eventBus;
    }

    Observable<PlayQueueUIItemsUpdate> playQueueUIItemsUpdate() {
        return Observable.merge(trackChangedEvent(), queueShuffledEvent(), itemsAddedEvent())
                         .startWith(PlayQueueUIItemsUpdate.forQueueLoad())
                         .flatMapSingle(event -> getTracksAndTitles().map(event::withItems));
    }

    @NonNull
    private Single<List<PlayQueueUIItem>> getTracksAndTitles() {
        return Single.zip(playQueueOperations.getTracks(),
                          playQueueOperations.getContextTitles(),
                          playQueueUIItemMapper);
    }

    //receives event when magic box is clicked
    private Observable<PlayQueueUIItemsUpdate> trackChangedEvent() {
        return eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                       .map(it -> PlayQueueUIItemsUpdate.forTrackChanged());
    }

    private Observable<PlayQueueUIItemsUpdate> queueShuffledEvent() {
        return eventBus.queue(EventQueue.PLAY_QUEUE)
                       .filter(PlayQueueEvent::isQueueReorder)
                       .map(it -> PlayQueueUIItemsUpdate.forQueueReorder());
    }

    private Observable<PlayQueueUIItemsUpdate> itemsAddedEvent() {
        return eventBus.queue(EventQueue.PLAY_QUEUE)
                       .filter(PlayQueueEvent::itemAdded)
                       .map(it -> PlayQueueUIItemsUpdate.forItemAdded());
    }

}
