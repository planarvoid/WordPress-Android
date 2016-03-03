package com.soundcloud.android.playlists;

import static com.soundcloud.java.collections.Iterables.getLast;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.OperatorSwitchOnEmptyList;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.rx.Pager;
import com.soundcloud.rx.Pager.PagingFunction;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

class PlaylistPostOperations {

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private final PlaylistPostStorage playlistPostStorage;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;
    private final NetworkConnectionHelper networkConnectionHelper;
    private final EventBus eventBus;

    private final PagingFunction<List<PropertySet>> postedPlaylistsPager = new PagingFunction<List<PropertySet>>() {
        @Override
        public Observable<List<PropertySet>> call(List<PropertySet> result) {
            if (result.size() < PAGE_SIZE) {
                return Pager.finish();
            } else {
                return postedPlaylists(getLast(result).get(PostProperty.CREATED_AT).getTime());
            }
        }
    };

    private final Action1<List<PropertySet>> requestPlaylistsSyncAction = new Action1<List<PropertySet>>() {
        @Override
        public void call(List<PropertySet> propertySets) {
            if (networkConnectionHelper.isWifiConnected() && !propertySets.isEmpty()) {
                syncInitiator.requestPlaylistSync(propertySets);
            }
        }
    };

    private final Func1<Boolean, Observable<List<PropertySet>>> loadInitialPlaylistPosts = new Func1<Boolean, Observable<List<PropertySet>>>() {
        @Override
        public Observable<List<PropertySet>> call(Boolean aBoolean) {
            return playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)
                    .doOnNext(requestPlaylistsSyncAction)
                    .subscribeOn(scheduler);
        }
    };

    private final Action1<WriteResult> requestSystemSync = new Action1<WriteResult>() {
        @Override
        public void call(WriteResult changeResult) {
            syncInitiator.requestSystemSync();
        }
    };

    @Inject
    PlaylistPostOperations(PlaylistPostStorage playlistPostStorage,
                           SyncInitiator syncInitiator,
                           @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                           NetworkConnectionHelper networkConnectionHelper,
                           EventBus eventBus) {
        this.playlistPostStorage = playlistPostStorage;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
        this.networkConnectionHelper = networkConnectionHelper;
        this.eventBus = eventBus;
    }

    Observable<List<PropertySet>> postedPlaylists() {
        return postedPlaylists(Long.MAX_VALUE);
    }

    Observable<List<PropertySet>> updatedPostedPlaylists() {
        return sync().flatMap(loadInitialPlaylistPosts);
    }

    Observable<Boolean> sync() {
        return syncInitiator.refreshMyPlaylists();
    }

    PagingFunction<List<PropertySet>> pagingFunction() {
        return postedPlaylistsPager;
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
        return eventBus.publishAction1(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromEntityDeleted(urn));
    }

    private Observable<List<PropertySet>> postedPlaylists(long beforeTime) {
        return playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, beforeTime)
                .doOnNext(requestPlaylistsSyncAction)
                .subscribeOn(scheduler)
                .lift(new OperatorSwitchOnEmptyList<>(updatedPostedPlaylists()));
    }
}
