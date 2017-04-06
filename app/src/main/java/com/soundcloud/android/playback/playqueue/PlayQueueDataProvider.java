package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.List;

public class PlayQueueDataProvider {

    private final PlayQueueOperations playQueueOperations;
    private final PlayQueueUIItemMapper playQueueUIItemMapper;
    private final EventBus eventBus;

    @Inject
    public PlayQueueDataProvider(PlayQueueOperations playQueueOperations, PlayQueueUIItemMapper playQueueUIItemMapper, EventBus eventBus) {
        this.playQueueOperations = playQueueOperations;
        this.playQueueUIItemMapper = playQueueUIItemMapper;
        this.eventBus = eventBus;
    }

    public Observable<PlayQueueUIItemsUpdate> playQueueUIItemsUpdate() {
        return Observable.merge(trackChangedEvent(), queueShuffledEvent(), itemsAddedEvent())
                         .startWith(PlayQueueUIItemsUpdate.forQueueLoad())
                         .flatMap(event -> getTracksAndTitles().map(event::withItems));
    }

    @NonNull
    private Observable<List<PlayQueueUIItem>> getTracksAndTitles() {
        return Observable.zip(playQueueOperations.getTracks(),
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
