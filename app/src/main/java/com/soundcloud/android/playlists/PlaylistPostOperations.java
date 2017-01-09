package com.soundcloud.android.playlists;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;

class PlaylistPostOperations {

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private final PlaylistPostStorage playlistPostStorage;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;
    private final EventBus eventBus;

    private final Action1<WriteResult> requestSystemSync = new Action1<WriteResult>() {
        @Override
        public void call(WriteResult changeResult) {
            syncInitiator.requestSystemSync();
        }
    };

    @Inject
    PlaylistPostOperations(PlaylistPostStorage playlistPostStorage,
                           @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                           SyncInitiator syncInitiator,
                           EventBus eventBus) {
        this.playlistPostStorage = playlistPostStorage;
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
        this.eventBus = eventBus;
    }

    Observable<Void> remove(final Urn urn) {
        final Observable<? extends WriteResult> remove = urn.isLocal()
                                                         ? playlistPostStorage.remove(urn)
                                                         : playlistPostStorage.markPendingRemoval(urn);
        return remove
                .doOnNext(publishPlaylistDeletedEvent(urn))
                .doOnNext(requestSystemSync)
                .map(RxUtils.TO_VOID)
                .subscribeOn(scheduler);
    }

    private Action1<WriteResult> publishPlaylistDeletedEvent(final Urn urn) {
        return eventBus.publishAction1(EventQueue.URN_STATE_CHANGED, UrnStateChangedEvent.fromEntityDeleted(urn));
    }


}
