package com.soundcloud.android.collection;

import static com.soundcloud.android.ApplicationModule.LOW_PRIORITY;

import com.soundcloud.android.Consts;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

@Singleton
public class PlayHistoryController {

    private static final long LONG_PLAY_MINIMAL_DURATION = TimeUnit.SECONDS.toMillis(10);
    private static final int NEW_PLAY_START_POSITION_THRESHOLD = 100;

    private final Func1<Holder, PlayHistoryRecord> toPlayHistoryRecord =
            new Func1<Holder, PlayHistoryRecord>() {
                @Override
                public PlayHistoryRecord call(Holder holder) {
                    return PlayHistoryRecord.create(currentStartedAt, holder.trackUrn, holder.collectionUrn);
                }
            };

    private final EventBus eventBus;
    private final DateProvider dateProvider;
    private final WritePlayHistoryCommand storeCommand;
    private final Scheduler scheduler;

    private Urn lastLongPlayed = Urn.NOT_SET;
    private Urn currentPlayed = Urn.NOT_SET;
    private long currentStartedAt = Consts.NOT_SET;

    @Inject
    PlayHistoryController(EventBus eventBus,
                          CurrentDateProvider dateProvider,
                          WritePlayHistoryCommand storeCommand,
                          @Named(LOW_PRIORITY) Scheduler scheduler) {
        this.eventBus = eventBus;
        this.dateProvider = dateProvider;
        this.storeCommand = storeCommand;
        this.scheduler = scheduler;
    }

    public void subscribe() {
        Observable
                .combineLatest(
                        eventBus.queue(EventQueue.PLAY_QUEUE),
                        eventBus.queue(EventQueue.PLAYBACK_PROGRESS),
                        combineEventsToHolder())
                .filter(isNewLongPlay())
                .map(toPlayHistoryRecord)
                .doOnNext(storeCommand.toAction1())
                .retry()
                .subscribeOn(scheduler)
                .subscribe();
    }

    private Func2<PlayQueueEvent, PlaybackProgressEvent, Holder> combineEventsToHolder() {
        return new Func2<PlayQueueEvent, PlaybackProgressEvent, Holder>() {
            @Override
            public Holder call(PlayQueueEvent playQueueEvent, PlaybackProgressEvent progressEvent) {
                return new Holder(
                        progressEvent.getPlaybackProgress().getPosition(),
                        progressEvent.getUrn(),
                        playQueueEvent.getCollectionUrn());
            }
        };
    }

    private Func1<Holder, Boolean> isNewLongPlay() {
        return new Func1<Holder, Boolean>() {
            @Override
            public Boolean call(Holder holder) {
                if (isNewCurrentPlay(holder)) {
                    currentPlayed = holder.trackUrn;
                    lastLongPlayed = Urn.NOT_SET;
                    currentStartedAt = dateProvider.getCurrentTime();
                } else if (isNewLongPlay(holder)) {
                    lastLongPlayed = holder.trackUrn;
                    return true;
                }
                return false;
            }
        };
    }

    private boolean isNewCurrentPlay(Holder holder) {
        return !currentPlayed.equals(holder.trackUrn)
                && holder.position <= NEW_PLAY_START_POSITION_THRESHOLD;
    }

    private boolean isNewLongPlay(Holder holder) {
        return !lastLongPlayed.equals(holder.trackUrn)
                && currentPlayed.equals(holder.trackUrn)
                && holder.position >= LONG_PLAY_MINIMAL_DURATION
                && getCurrentPlayedTime() >= LONG_PLAY_MINIMAL_DURATION;
    }

    private long getCurrentPlayedTime() {
        return dateProvider.getCurrentTime() - currentStartedAt;
    }

    private class Holder {
        private long position;
        private Urn trackUrn;
        private Urn collectionUrn;

        Holder(long position, Urn trackUrn, Urn collectionUrn) {
            this.position = position;
            this.trackUrn = trackUrn;
            this.collectionUrn = collectionUrn;
        }
    }

}
