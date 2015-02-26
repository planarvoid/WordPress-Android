package com.soundcloud.android.likes;

import static com.google.common.collect.Iterables.getLast;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
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
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;
    private final EventBus eventBus;
    private final NetworkConnectionHelper networkConnectionHelper;

    // TODO: revert this to create a new instance in the respective method once we port
    // the playlist likes fragment to the new ListPresenter
    private final Pager<List<PropertySet>> likedPlaylistsPager = new Pager<List<PropertySet>>() {
        @Override
        public Observable<List<PropertySet>> call(List<PropertySet> result) {
            if (result.size() < PAGE_SIZE) {
                return Pager.finish();
            } else {
                return likedPlaylists(getLast(result).get(LikeProperty.CREATED_AT).getTime());
            }
        }
    };

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
                                  SyncInitiator syncInitiator,
                                  EventBus eventBus,
                                  @Named("Storage") Scheduler scheduler,
                                  NetworkConnectionHelper networkConnectionHelper) {
        this.loadLikedPlaylistsCommand = loadLikedPlaylistsCommand;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
        this.networkConnectionHelper = networkConnectionHelper;
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
        return likedPlaylistsPager;
    }
}
