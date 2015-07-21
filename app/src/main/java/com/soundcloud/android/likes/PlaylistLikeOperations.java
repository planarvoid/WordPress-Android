package com.soundcloud.android.likes;

import static com.google.common.collect.Iterables.getLast;
import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.OperatorSwitchOnEmptyList;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.rx.Pager;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class PlaylistLikeOperations {

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private final PlaylistLikesStorage storage;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;
    private final EventBus eventBus;
    private final NetworkConnectionHelper networkConnectionHelper;

    private final Action1<List<PropertySet>> requestPlaylistsSyncAction = new Action1<List<PropertySet>>() {
        @Override
        public void call(List<PropertySet> propertySets) {
            if (networkConnectionHelper.isWifiConnected() && !propertySets.isEmpty()) {
                syncInitiator.requestPlaylistSync(propertySets);
            }
        }
    };

    private final Func1<Urn, Observable<PropertySet>> toLoadLikedPlaylist = new Func1<Urn, Observable<PropertySet>>() {
        @Override
        public Observable<PropertySet> call(Urn urn) {
            return storage.loadLikedPlaylist(urn).subscribeOn(scheduler);
        }
    };

    private final Func1<SyncResult, Observable<List<PropertySet>>> toLoadLikedPlaylists = new Func1<SyncResult, Observable<List<PropertySet>>>() {
        @Override
        public Observable<List<PropertySet>> call(SyncResult syncResult) {
            return storage.loadLikedPlaylists(PAGE_SIZE, Long.MAX_VALUE).subscribeOn(scheduler);
        }
    };

    @Inject
    PlaylistLikeOperations(PlaylistLikesStorage storage, SyncInitiator syncInitiator,
                           EventBus eventBus,
                           @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                           NetworkConnectionHelper networkConnectionHelper) {
        this.storage = storage;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
        this.networkConnectionHelper = networkConnectionHelper;
    }

    public Observable<PropertySet> onPlaylistLiked() {
        return eventBus.queue(ENTITY_STATE_CHANGED)
                .filter(EntityStateChangedEvent.IS_PLAYLIST_LIKED_FILTER)
                .map(EntityStateChangedEvent.TO_URN)
                .flatMap(toLoadLikedPlaylist);
    }

    public Observable<Urn> onPlaylistUnliked() {
        return eventBus.queue(ENTITY_STATE_CHANGED)
                .filter(EntityStateChangedEvent.IS_PLAYLIST_UNLIKED_FILTER)
                .map(EntityStateChangedEvent.TO_URN);
    }

    public Observable<List<PropertySet>> likedPlaylists() {
        return likedPlaylists(Long.MAX_VALUE);
    }

    public Observable<List<PropertySet>> updatedLikedPlaylists() {
        return syncInitiator.syncPlaylistLikes().flatMap(toLoadLikedPlaylists);
    }

    private Observable<List<PropertySet>> likedPlaylists(long beforeTime) {
        return loadLikedPlaylistsInternal(beforeTime)
                .lift(new OperatorSwitchOnEmptyList<>(updatedLikedPlaylists()));
    }

    private Observable<List<PropertySet>> loadLikedPlaylistsInternal(long beforeTime) {
        return storage.loadLikedPlaylists(PAGE_SIZE, beforeTime)
                .doOnNext(requestPlaylistsSyncAction)
                .subscribeOn(scheduler);
    }

    public Pager.PagingFunction<List<PropertySet>> pagingFunction() {
        return new Pager.PagingFunction<List<PropertySet>>() {
            @Override
            public Observable<List<PropertySet>> call(List<PropertySet> result) {
                if (result.size() < PAGE_SIZE) {
                    return Pager.finish();
                } else {
                    return loadLikedPlaylistsInternal(getLast(result).get(LikeProperty.CREATED_AT).getTime());
                }
            }
        };
    }
}
