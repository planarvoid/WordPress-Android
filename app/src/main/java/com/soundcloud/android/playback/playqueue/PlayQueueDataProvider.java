package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.rx.eventbus.EventBus;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;

import java.util.List;

public class PlayQueueDataProvider {

    private final PlayQueueOperations playQueueOperations;
    private final PlayQueueUIItemMapper playQueueUIItemMapper;
    private final EventBus eventBus;
    private final CompositeSubscription subscriptions = new CompositeSubscription();
    private final Subject<Boolean, Boolean> updateSubject = PublishSubject.create();

    @Inject
    public PlayQueueDataProvider(PlayQueueOperations playQueueOperations, PlayQueueUIItemMapper playQueueUIItemMapper, EventBus eventBus) {
        this.playQueueOperations = playQueueOperations;
        this.playQueueUIItemMapper = playQueueUIItemMapper;
        this.eventBus = eventBus;
    }

    public Observable<List<PlayQueueUIItem>> getPlayQueueUIItems() {
        setUpTrackChangeStream();
        setUpShuffleStream();
        setUpItemAddedStream();
        return updateSubject.flatMap(event -> Observable.zip(playQueueOperations.getTracks(), playQueueOperations.getContextTitles(), playQueueUIItemMapper))
                            .startWith(playQueueOperations.getTracks().zipWith(playQueueOperations.getContextTitles(), playQueueUIItemMapper));
    }

    public void clearRemoveSubscriptions() {
        subscriptions.clear();
    }

    //receives event when magic box is clicked
    private void setUpTrackChangeStream() {
        subscriptions.add(eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                                  .map(it -> true)
                                  .subscribe(updateSubject));
    }

    private void setUpShuffleStream() {
        subscriptions.add(eventBus.queue(EventQueue.PLAY_QUEUE)
                                  .filter(playQueueEvent -> playQueueEvent.isQueueReorder())
                                  .map(it -> true)
                                  .subscribe(updateSubject));
    }

    private void setUpItemAddedStream() {
        subscriptions.add(eventBus.queue(EventQueue.PLAY_QUEUE)
                                  .filter(playQueueEvent -> playQueueEvent.itemAdded())
                                  .map(it -> true)
                                  .subscribe(updateSubject));
    }

}
