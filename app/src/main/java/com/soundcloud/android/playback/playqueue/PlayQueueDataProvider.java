package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

import javax.inject.Inject;
import java.util.List;

public class PlayQueueDataProvider {

    private final PlayQueueOperations playQueueOperations;
    private final PlayQueueUIItemMapper playQueueUIItemMapper;
    private final EventBus eventBus;
    private final Subject<Boolean, Boolean> updateSubject = PublishSubject.create();

    @Inject
    public PlayQueueDataProvider(PlayQueueOperations playQueueOperations, PlayQueueUIItemMapper playQueueUIItemMapper, EventBus eventBus) {
        this.playQueueOperations = playQueueOperations;
        this.playQueueUIItemMapper = playQueueUIItemMapper;
        this.eventBus = eventBus;
        setUpTrackChangeStream();
        setUpShuffleStream();
        setUpItemAddedStream();
    }

    public Observable<List<PlayQueueUIItem>> getPlayQueueUIItems() {
        return updateSubject.flatMap(event -> Observable.zip(playQueueOperations.getTracks(), playQueueOperations.getContextTitles(), playQueueUIItemMapper))
                            .startWith(playQueueOperations.getTracks().zipWith(playQueueOperations.getContextTitles(), playQueueUIItemMapper));
    }

    //receives event when magic box is clicked
    private void setUpTrackChangeStream() {
        eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                .skip(1) //replay is called so we ignore the first after subscribing
                .map(it -> true)
                .subscribe(updateSubject);
    }

    private void setUpShuffleStream() {
        eventBus.queue(EventQueue.PLAY_QUEUE)
                .filter(playQueueEvent -> playQueueEvent.isQueueReorder())
                .map(it -> true)
                .subscribe(updateSubject);
    }

    private void setUpItemAddedStream() {
        eventBus.queue(EventQueue.PLAY_QUEUE)
                .filter(playQueueEvent -> playQueueEvent.itemAdded())
                .map(it -> true)
                .subscribe(updateSubject);
    }

}
