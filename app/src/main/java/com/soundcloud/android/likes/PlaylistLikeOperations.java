package com.soundcloud.android.likes;

import static com.google.common.collect.Iterables.getLast;
import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.OperatorSwitchOnEmptyList;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.android.Pager;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class PlaylistLikeOperations {

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private final LoadLikedPlaylistsCommand loadLikedPlaylistsCommand;
    private final LoadLikedPlaylistCommand loadLikedPlaylistCommand;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;
    private final EventBus eventBus;
    private final NetworkConnectionHelper networkConnectionHelper;

    private final Func1<SyncResult, ChronologicalQueryParams> toInitalPageParams = new Func1<SyncResult, ChronologicalQueryParams>() {
        @Override
        public ChronologicalQueryParams call(SyncResult syncResult) {
            return new ChronologicalQueryParams(PAGE_SIZE, Long.MAX_VALUE);
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

    @Inject
    public PlaylistLikeOperations(LoadLikedPlaylistsCommand loadLikedPlaylistsCommand,
                                  LoadLikedPlaylistCommand loadLikedPlaylistCommand,
                                  SyncInitiator syncInitiator,
                                  EventBus eventBus,
                                  @Named("Storage") Scheduler scheduler,
                                  NetworkConnectionHelper networkConnectionHelper) {
        this.loadLikedPlaylistsCommand = loadLikedPlaylistsCommand;
        this.loadLikedPlaylistCommand = loadLikedPlaylistCommand;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
        this.networkConnectionHelper = networkConnectionHelper;
    }

    public Observable<PropertySet> onPlaylistLiked(){
        return eventBus.queue(ENTITY_STATE_CHANGED)
                .filter(EntityStateChangedEvent.IS_PLAYLIST_LIKED_FILTER)
                .map(EntityStateChangedEvent.TO_URN)
                .flatMap(loadLikedPlaylistCommand);
    }

    public Observable<Urn> onPlaylistUnliked() {
        return eventBus.queue(ENTITY_STATE_CHANGED)
                .filter(EntityStateChangedEvent.IS_PLAYLIST_UNLIKED_FILTER)
                .map(EntityStateChangedEvent.TO_URN);
    }


    public Observable<List<PropertySet>> likedPlaylists() {
        return likedPlaylists(Long.MAX_VALUE);
    }

    public Observable<List<PropertySet>> likedPlaylists(long beforeTime) {
        return loadLikedPlaylistsCommand
                .with(new ChronologicalQueryParams(PAGE_SIZE, beforeTime))
                .toObservable()
                .doOnNext(requestPlaylistsSyncAction)
                .subscribeOn(scheduler)
                .lift(new OperatorSwitchOnEmptyList<>(updatedLikedPlaylists()));
    }

    public Observable<List<PropertySet>> updatedLikedPlaylists() {
        return syncInitiator.syncPlaylistLikes().map(toInitalPageParams).flatMap(loadLikedPlaylistsCommand);
    }

    public Pager<List<PropertySet>> likedPlaylistsPager() {
        return new Pager<List<PropertySet>>() {
            @Override
            public Observable<List<PropertySet>> call(List<PropertySet> result) {
                if (result.size() < PAGE_SIZE) {
                    return Pager.finish();
                } else {
                    return likedPlaylists(getLast(result).get(LikeProperty.CREATED_AT).getTime());
                }
            }
        };
    }
}
