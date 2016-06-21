package com.soundcloud.android.collection;

import static com.soundcloud.android.ApplicationModule.LOW_PRIORITY;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ads.AdUtils;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayHistoryEvent;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

@Singleton
public class PlayHistoryController {

    private static final long LONG_PLAY_MINIMAL_DURATION_SECONDS = 1;

    private static final Func1<Pair<CurrentPlayQueueItemEvent, PlaybackStateTransition>, Boolean> IS_ELIGIBLE_FOR_HISTORY =
            new Func1<Pair<CurrentPlayQueueItemEvent, PlaybackStateTransition>, Boolean>() {
                @Override
                public Boolean call(Pair<CurrentPlayQueueItemEvent, PlaybackStateTransition> pair) {
                    CurrentPlayQueueItemEvent playQueueEvent = pair.first();
                    PlayQueueItem currentPlayQueueItem = playQueueEvent.getCurrentPlayQueueItem();
                    PlaybackStateTransition stateTransition = pair.second();

                    return stateTransition.isPlayerPlaying()
                            && !PlayQueueItem.EMPTY.equals(currentPlayQueueItem)
                            && currentPlayQueueItem.getUrn().equals(stateTransition.getUrn())
                            && !AdUtils.isAd(currentPlayQueueItem);
                }
            };

    private static final Func2<CurrentPlayQueueItemEvent, PlaybackStateTransition, Pair<CurrentPlayQueueItemEvent, PlaybackStateTransition>> COMBINE_EVENTS =
            new Func2<CurrentPlayQueueItemEvent, PlaybackStateTransition, Pair<CurrentPlayQueueItemEvent, PlaybackStateTransition>>() {
                @Override
                public Pair<CurrentPlayQueueItemEvent, PlaybackStateTransition> call(CurrentPlayQueueItemEvent queueItemEvent, PlaybackStateTransition stateTransition) {
                    return Pair.of(queueItemEvent, stateTransition);
                }
            };

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
                        COMBINE_EVENTS)
                .doOnNext(unsubscribePreviousTimer())
                .filter(IS_ELIGIBLE_FOR_HISTORY)
                .subscribeOn(scheduler)
                .subscribe(new PlayHistorySubscriber());
    }

    private Action1<Pair<CurrentPlayQueueItemEvent, PlaybackStateTransition>> unsubscribePreviousTimer() {
        return new Action1<Pair<CurrentPlayQueueItemEvent, PlaybackStateTransition>>() {
            @Override
            public void call(Pair<CurrentPlayQueueItemEvent, PlaybackStateTransition> currentPlayQueueItemEventPlaybackStateTransitionPair) {
                subscription.unsubscribe();
            }
        };
    }

    private PlayHistoryRecord buildRecord(CurrentPlayQueueItemEvent queueItemEvent, PlaybackStateTransition stateTransition) {
        return PlayHistoryRecord.create(
                stateTransition.getProgress().getCreatedAt(),
                queueItemEvent.getCurrentPlayQueueItem().getUrn(),
                queueItemEvent.getCollectionUrn());
    }

    private void schedulePlayHistoryStorage(PlayHistoryRecord record) {
        subscription = Observable.timer(LONG_PLAY_MINIMAL_DURATION_SECONDS, TimeUnit.SECONDS, scheduler)
                .flatMap(continueWith(Observable.just(record)))
                .doOnNext(storeCommand.toAction1())
                .doOnNext(publishNewPlayHistory())
                .subscribeOn(scheduler)
                .subscribe(new DefaultSubscriber<PlayHistoryRecord>());
    }

    @NonNull
    private Action1<PlayHistoryRecord> publishNewPlayHistory() {
        return new Action1<PlayHistoryRecord>() {
            @Override
            public void call(PlayHistoryRecord record) {
                eventBus.publish(EventQueue.PLAY_HISTORY, PlayHistoryEvent.fromAdded(record.trackUrn()));
            }
        };
    }

    private class PlayHistorySubscriber extends DefaultSubscriber<Pair<CurrentPlayQueueItemEvent, PlaybackStateTransition>> {
        @Override
        public void onNext(Pair<CurrentPlayQueueItemEvent, PlaybackStateTransition> pair) {
            PlayHistoryRecord record = buildRecord(pair.first(), pair.second());
            schedulePlayHistoryStorage(record);
        }
    }
}
