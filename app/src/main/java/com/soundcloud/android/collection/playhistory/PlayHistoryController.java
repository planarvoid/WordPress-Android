package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.ApplicationModule.LOW_PRIORITY;

import com.soundcloud.android.ads.AdUtils;
import com.soundcloud.android.collection.recentlyplayed.WriteRecentlyPlayedCommand;
import com.soundcloud.android.configuration.experiments.PlayHistoryExperiment;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayHistoryEvent;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class PlayHistoryController {

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

    private static final Func1<Pair<CurrentPlayQueueItemEvent, PlayStateEvent>, PlayHistoryRecord> TO_PLAY_HISTORY_RECORD =
            new Func1<Pair<CurrentPlayQueueItemEvent, PlayStateEvent>, PlayHistoryRecord>() {
                @Override
                public PlayHistoryRecord call(Pair<CurrentPlayQueueItemEvent, PlayStateEvent> pair) {
                    CurrentPlayQueueItemEvent queueItemEvent = pair.first();
                    PlayStateEvent playStateEvent = pair.second();

                    return PlayHistoryRecord.create(
                            playStateEvent.getProgress().getCreatedAt(),
                            queueItemEvent.getCurrentPlayQueueItem().getUrn(),
                            queueItemEvent.getCollectionUrn());
                }
            };

    private final EventBus eventBus;
    private final WritePlayHistoryCommand playHistoryStoreCommand;
    private final WriteRecentlyPlayedCommand recentlyPlayedStoreCommand;
    private final PlayHistoryExperiment playHistoryExperiment;
    private final Scheduler scheduler;

    @Inject
    public PlayHistoryController(EventBus eventBus,
                                 WritePlayHistoryCommand playHistoryStoreCommand,
                                 WriteRecentlyPlayedCommand recentlyPlayedStoreCommand,
                                 PlayHistoryExperiment playHistoryExperiment,
                                 @Named(LOW_PRIORITY) Scheduler scheduler) {
        this.eventBus = eventBus;
        this.playHistoryStoreCommand = playHistoryStoreCommand;
        this.recentlyPlayedStoreCommand = recentlyPlayedStoreCommand;
        this.playHistoryExperiment = playHistoryExperiment;
        this.scheduler = scheduler;
    }

    public void subscribe() {
        Observable
                .combineLatest(
                        eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM),
                        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED),
                        COMBINE_EVENTS)
                .observeOn(scheduler)
                .filter(IS_ELIGIBLE_FOR_HISTORY)
                .map(TO_PLAY_HISTORY_RECORD)
                .doOnNext(playHistoryStoreCommand.toAction1())
                .doOnNext(recentlyPlayedStoreCommand.toAction1())
                .doOnNext(publishNewPlayHistory())
                .subscribe(new DefaultSubscriber<PlayHistoryRecord>());
    }

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
}