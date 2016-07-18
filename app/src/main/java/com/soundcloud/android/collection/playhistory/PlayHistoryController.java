package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.ApplicationModule.LOW_PRIORITY;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ads.AdUtils;
import com.soundcloud.android.configuration.experiments.PlayHistoryExperiment;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayHistoryEvent;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayStateEvent;
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

    private static final Func1<Pair<CurrentPlayQueueItemEvent, PlayStateEvent>, Boolean> IS_ELIGIBLE_FOR_HISTORY =
            new Func1<Pair<CurrentPlayQueueItemEvent, PlayStateEvent>, Boolean>() {
                @Override
                public Boolean call(Pair<CurrentPlayQueueItemEvent, PlayStateEvent> pair) {
                    CurrentPlayQueueItemEvent playQueueEvent = pair.first();
                    PlayQueueItem currentPlayQueueItem = playQueueEvent.getCurrentPlayQueueItem();
                    PlayStateEvent playStateEvent = pair.second();

                    return playStateEvent.isPlayerPlaying()
                            && !PlayQueueItem.EMPTY.equals(currentPlayQueueItem)
                            && currentPlayQueueItem.getUrn().equals(playStateEvent.getPlayingItemUrn())
                            && !AdUtils.isAd(currentPlayQueueItem);
                }
            };

    private static final Func2<CurrentPlayQueueItemEvent, PlayStateEvent, Pair<CurrentPlayQueueItemEvent, PlayStateEvent>> COMBINE_EVENTS =
            new Func2<CurrentPlayQueueItemEvent, PlayStateEvent, Pair<CurrentPlayQueueItemEvent, PlayStateEvent>>() {
                @Override
                public Pair<CurrentPlayQueueItemEvent, PlayStateEvent> call(CurrentPlayQueueItemEvent queueItemEvent,
                                                                            PlayStateEvent playStateEvent) {
                    return Pair.of(queueItemEvent, playStateEvent);
                }
            };

    private final EventBus eventBus;
    private final WritePlayHistoryCommand storeCommand;
    private final PlayHistoryExperiment playHistoryExperiment;
    private final Scheduler scheduler;

    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    public PlayHistoryController(EventBus eventBus,
                          WritePlayHistoryCommand storeCommand,
                          PlayHistoryExperiment playHistoryExperiment,
                          @Named(LOW_PRIORITY) Scheduler scheduler) {
        this.eventBus = eventBus;
        this.storeCommand = storeCommand;
        this.playHistoryExperiment = playHistoryExperiment;
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

    private Action1<Pair<CurrentPlayQueueItemEvent, PlayStateEvent>> unsubscribePreviousTimer() {
        return new Action1<Pair<CurrentPlayQueueItemEvent, PlayStateEvent>>() {
            @Override
            public void call(Pair<CurrentPlayQueueItemEvent, PlayStateEvent> ignored) {
                subscription.unsubscribe();
            }
        };
    }

    private PlayHistoryRecord buildRecord(CurrentPlayQueueItemEvent queueItemEvent,
                                          PlayStateEvent playStateEvent) {
        return PlayHistoryRecord.create(
                playStateEvent.getProgress().getCreatedAt(),
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
                if (playHistoryExperiment.isEnabled()) {
                    eventBus.publish(EventQueue.PLAY_HISTORY, PlayHistoryEvent.fromAdded(record.trackUrn()));
                }
            }
        };
    }

    private class PlayHistorySubscriber
            extends DefaultSubscriber<Pair<CurrentPlayQueueItemEvent, PlayStateEvent>> {
        @Override
        public void onNext(Pair<CurrentPlayQueueItemEvent, PlayStateEvent> pair) {
            PlayHistoryRecord record = buildRecord(pair.first(), pair.second());
            schedulePlayHistoryStorage(record);
        }
    }
}
