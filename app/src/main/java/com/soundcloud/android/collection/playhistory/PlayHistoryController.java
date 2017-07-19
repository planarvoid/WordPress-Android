package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.ApplicationModule.RX_LOW_PRIORITY;

import com.soundcloud.android.collection.recentlyplayed.PushRecentlyPlayedCommand;
import com.soundcloud.android.collection.recentlyplayed.WriteRecentlyPlayedCommand;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayHistoryEvent;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class PlayHistoryController {

    private static final Predicate<Pair<CurrentPlayQueueItemEvent, PlayStateEvent>> IS_ELIGIBLE_FOR_HISTORY =
            pair -> {
                CurrentPlayQueueItemEvent playQueueEvent = pair.first();
                PlayQueueItem currentPlayQueueItem = playQueueEvent.getCurrentPlayQueueItem();
                PlayStateEvent playStateEvent = pair.second();

                return playStateEvent.isPlayerPlaying()
                        && !PlayQueueItem.EMPTY.equals(currentPlayQueueItem)
                        && currentPlayQueueItem.getUrn().equals(playStateEvent.getPlayingItemUrn())
                        && !currentPlayQueueItem.isAd();
            };

    private static final Function<Pair<CurrentPlayQueueItemEvent, PlayStateEvent>, PlayHistoryRecord> TO_PLAY_HISTORY_RECORD =
            pair -> {
                CurrentPlayQueueItemEvent queueItemEvent = pair.first();
                PlayStateEvent playStateEvent = pair.second();

                return PlayHistoryRecord.create(
                        playStateEvent.getProgress().getCreatedAt(),
                        queueItemEvent.getCurrentPlayQueueItem().getUrn(),
                        queueItemEvent.getCollectionUrn());
            };

    private final EventBusV2 eventBus;
    private final WritePlayHistoryCommand playHistoryStoreCommand;
    private final WriteRecentlyPlayedCommand recentlyPlayedStoreCommand;
    private final PushPlayHistoryCommand pushPlayHistoryCommand;
    private final PushRecentlyPlayedCommand pushRecentlyPlayedCommand;
    private final Scheduler scheduler;

    @Inject
    public PlayHistoryController(EventBusV2 eventBus,
                                 WritePlayHistoryCommand playHistoryStoreCommand,
                                 WriteRecentlyPlayedCommand recentlyPlayedStoreCommand,
                                 PushPlayHistoryCommand pushPlayHistoryCommand,
                                 PushRecentlyPlayedCommand pushRecentlyPlayedCommand,
                                 @Named(RX_LOW_PRIORITY) Scheduler scheduler) {
        this.eventBus = eventBus;
        this.playHistoryStoreCommand = playHistoryStoreCommand;
        this.recentlyPlayedStoreCommand = recentlyPlayedStoreCommand;
        this.pushPlayHistoryCommand = pushPlayHistoryCommand;
        this.pushRecentlyPlayedCommand = pushRecentlyPlayedCommand;
        this.scheduler = scheduler;
    }

    public void subscribe() {
        Observable
                .combineLatest(
                        eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM),
                        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED),
                        Pair::of)
                .observeOn(scheduler)
                .filter(IS_ELIGIBLE_FOR_HISTORY)
                .map(TO_PLAY_HISTORY_RECORD)
                .doOnNext(playHistoryStoreCommand.toConsumer())
                .doOnNext(recentlyPlayedStoreCommand.toConsumer())
                .doOnNext(publishNewPlayHistory())
                .doOnNext(pushPlayHistoryCommand.toConsumer())
                .doOnNext(pushRecentlyPlayedCommand.toConsumer())
                .subscribe(new DefaultObserver<>());
    }

    private Consumer<PlayHistoryRecord> publishNewPlayHistory() {
        return record -> eventBus.publish(EventQueue.PLAY_HISTORY, PlayHistoryEvent.fromAdded(record.trackUrn()));
    }
}
