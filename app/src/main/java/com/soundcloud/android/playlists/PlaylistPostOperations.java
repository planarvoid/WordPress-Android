package com.soundcloud.android.playlists;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxSignal;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Observable;
import io.reactivex.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;

class PlaylistPostOperations {

    private final PostsStorage postsStorage;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;
    private final EventBusV2 eventBus;

    @Inject
    PlaylistPostOperations(PostsStorage postsStorage,
                           @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                           SyncInitiator syncInitiator,
                           EventBusV2 eventBus) {
        this.postsStorage = postsStorage;
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
        this.eventBus = eventBus;
    }

    Observable<RxSignal> remove(final Urn urn) {
        final Observable<TxnResult> remove = urn.isLocal()
                                             ? postsStorage.remove(urn)
                                             : postsStorage.markPendingRemoval(urn);
        return remove
                .flatMapSingle(__ -> syncInitiator.requestSystemSync().toSingle(() -> RxSignal.SIGNAL))
                .doOnNext(eventBus.publishAction1(EventQueue.URN_STATE_CHANGED, UrnStateChangedEvent.fromEntityDeleted(urn)))
                .subscribeOn(scheduler);
    }


}
