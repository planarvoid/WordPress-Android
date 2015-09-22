package com.soundcloud.android.playlists;


import static com.soundcloud.java.collections.Iterables.getLast;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.rx.OperatorSwitchOnEmptyList;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.Pager;
import com.soundcloud.rx.Pager.PagingFunction;
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

    @Inject
    PlaylistPostOperations(PlaylistPostStorage playlistPostStorage,
                           SyncInitiator syncInitiator,
                           @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                           NetworkConnectionHelper networkConnectionHelper) {
        this.playlistPostStorage = playlistPostStorage;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
        this.networkConnectionHelper = networkConnectionHelper;
    }

    Observable<List<PropertySet>> postedPlaylists() {
        return postedPlaylists(Long.MAX_VALUE);
    }

    Observable<List<PropertySet>> updatedPostedPlaylists() {
        return syncInitiator.refreshPostedPlaylists()
                .flatMap(loadInitialPlaylistPosts);
    }

    PagingFunction<List<PropertySet>> pagingFunction() {
        return postedPlaylistsPager;
    }

    private Observable<List<PropertySet>> postedPlaylists(long beforeTime) {
        return playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, beforeTime)
                .doOnNext(requestPlaylistsSyncAction)
                .subscribeOn(scheduler)
                .lift(new OperatorSwitchOnEmptyList<>(updatedPostedPlaylists()));
    }
}
