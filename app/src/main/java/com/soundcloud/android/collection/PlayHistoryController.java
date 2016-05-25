package com.soundcloud.android.collection;

import static com.soundcloud.android.ApplicationModule.LOW_PRIORITY;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ads.AdUtils;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

@Singleton
public class PlayHistoryController {

    private static final long LONG_PLAY_MINIMAL_DURATION_SECONDS = 10;

    private final EventBus eventBus;
    private final WritePlayHistoryCommand storeCommand;
    private final Scheduler scheduler;

    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    PlayHistoryController(EventBus eventBus,
                          WritePlayHistoryCommand storeCommand,
                          @Named(LOW_PRIORITY) Scheduler scheduler) {
        this.eventBus = eventBus;
        this.storeCommand = storeCommand;
        this.scheduler = scheduler;
    }

    public void subscribe() {
        Observable
                .combineLatest(
                        eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM),
                        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED),
                        combineEventsToHolder())
                .subscribeOn(scheduler)
                .subscribe();
    }

    private Func2<CurrentPlayQueueItemEvent, PlaybackStateTransition, Void> combineEventsToHolder() {
        return new Func2<CurrentPlayQueueItemEvent, PlaybackStateTransition, Void>() {
            @Override
            public Void call(CurrentPlayQueueItemEvent queueItemEvent, PlaybackStateTransition stateTransition) {
                subscription.unsubscribe();
                addPlayHistory(queueItemEvent, stateTransition);
                return null;
            }
        };
    }

    private void addPlayHistory(CurrentPlayQueueItemEvent queueItemEvent, PlaybackStateTransition stateTransition) {
        if (canAddPlayHistory(stateTransition, queueItemEvent)) {
            PlayHistoryRecord record = buildRecord(stateTransition, queueItemEvent);
            schedulePlayHistoryStorage(record);
        }
    }

    private boolean canAddPlayHistory(PlaybackStateTransition stateTransition, CurrentPlayQueueItemEvent queueItemEvent) {
        PlayQueueItem currentPlayQueueItem = queueItemEvent.getCurrentPlayQueueItem();
        return stateTransition.isPlayerPlaying()
                && currentPlayQueueItem.getUrn().equals(stateTransition.getUrn())
                && !AdUtils.isAd(currentPlayQueueItem);
    }

    private PlayHistoryRecord buildRecord(PlaybackStateTransition stateTransition, CurrentPlayQueueItemEvent queueItemEvent) {
        return PlayHistoryRecord.create(
                stateTransition.getProgress().getCreatedAt(),
                queueItemEvent.getCurrentPlayQueueItem().getUrn(),
                queueItemEvent.getCollectionUrn());
    }

    private void schedulePlayHistoryStorage(PlayHistoryRecord record) {
        subscription = Observable.timer(LONG_PLAY_MINIMAL_DURATION_SECONDS, TimeUnit.SECONDS, scheduler)
                .flatMap(continueWith(Observable.just(record)))
                .doOnNext(storeCommand.toAction1())
                .subscribeOn(scheduler)
                .subscribe(new DefaultSubscriber<PlayHistoryRecord>());
    }


}
