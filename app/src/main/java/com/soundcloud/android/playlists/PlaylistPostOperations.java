package com.soundcloud.android.playlists;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.posts.PostsStorage;
import com.soundcloud.android.rx.RxSignal;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Scheduler;
import io.reactivex.Single;

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

    Single<RxSignal> remove(final Urn urn) {
        final Single<TxnResult> remove = urn.isLocal()
                                         ? postsStorage.removePlaylist(urn)
                                         : postsStorage.markPlaylistPendingRemoval(urn);
        return remove
                .flatMap(__ -> syncInitiator.requestSystemSync().toSingle(() -> RxSignal.SIGNAL))
                .doOnSuccess(eventBus.publishAction1(EventQueue.URN_STATE_CHANGED, UrnStateChangedEvent.fromEntityDeleted(urn)))
                .subscribeOn(scheduler);
    }


}
