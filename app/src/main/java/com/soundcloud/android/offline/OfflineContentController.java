package com.soundcloud.android.offline;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.propeller.TxnResult;
import rx.Observable;
import rx.functions.Func1;

import android.content.Context;

import javax.inject.Inject;

public class OfflineContentController {
    private final EventBus eventBus;
    private final Context context;
    private final OfflineContentOperations operations;

    public static final Func1<PlayableUpdatedEvent, Boolean> IS_TRACK_LIKED_FILTER = new Func1<PlayableUpdatedEvent, Boolean>() {
        @Override
        public Boolean call(PlayableUpdatedEvent event) {
            return event.getUrn().isTrack()
                    && event.isFromLike()
                    && event.getChangeSet().get(PlayableProperty.IS_LIKED);
        }
    };

    private final Func1<PlayableUpdatedEvent, Observable<TxnResult>> updateOfflineLikes = new Func1<PlayableUpdatedEvent, Observable<TxnResult>>() {
        @Override
        public Observable<TxnResult> call(PlayableUpdatedEvent playableUpdatedEvent) {
            return operations.updateOfflineLikes();
        }
    };

    @Inject
    public OfflineContentController(EventBus eventBus, OfflineContentOperations operations, Context context) {
        this.eventBus = eventBus;
        this.operations = operations;
        this.context = context;
    }

    public void subscribe() {
        eventBus.queue(EventQueue.PLAYABLE_CHANGED)
                .filter(IS_TRACK_LIKED_FILTER)
                .flatMap(updateOfflineLikes)
                .subscribe(new StartOfflineContentServiceSubscriber());
    }

    private final class StartOfflineContentServiceSubscriber extends DefaultSubscriber<TxnResult> {
        @Override
        public void onNext(TxnResult tracks) {
            OfflineContentService.syncOfflineContent(context);
        }
    }
}
