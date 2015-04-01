package com.soundcloud.android.playlists;


import static com.google.common.collect.Iterables.getLast;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.commands.PagedQueryCommand;
import com.soundcloud.android.likes.ChronologicalQueryParams;
import com.soundcloud.android.rx.OperatorSwitchOnEmptyList;
import com.soundcloud.android.sync.SyncInitiator;
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

class PlaylistPostOperations {

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private final PagedQueryCommand<ChronologicalQueryParams> loadPostedPlaylistsCommand;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;
    private final NetworkConnectionHelper networkConnectionHelper;

    private final Pager<List<PropertySet>> postedPlaylistsPager = new Pager<List<PropertySet>>() {
        @Override
        public Observable<List<PropertySet>> call(List<PropertySet> result) {
            if (result.size() < PAGE_SIZE) {
                return Pager.finish();
            } else {
                return postedPlaylists(getLast(result).get(PlaylistProperty.CREATED_AT).getTime());
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

    private final Func1<Object, ChronologicalQueryParams> toInitalPageParams = new Func1<Object, ChronologicalQueryParams>() {
        @Override
        public ChronologicalQueryParams call(Object unused) {
            return new ChronologicalQueryParams(PAGE_SIZE, Long.MAX_VALUE);
        }
    };

    @Inject
    PlaylistPostOperations(@Named("LoadPostedPlaylistsCommand") PagedQueryCommand<ChronologicalQueryParams> loadPostedPlaylistsCommand,
                           SyncInitiator syncInitiator,
                           @Named("HighPriority") Scheduler scheduler,
                           NetworkConnectionHelper networkConnectionHelper) {
        this.loadPostedPlaylistsCommand = loadPostedPlaylistsCommand;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
        this.networkConnectionHelper = networkConnectionHelper;
    }

    Observable<List<PropertySet>> postedPlaylists() {
        return postedPlaylists(Long.MAX_VALUE);
    }

    Observable<List<PropertySet>> updatedPostedPlaylists() {
        return syncInitiator.refreshPostedPlaylists().map(toInitalPageParams).flatMap(loadPostedPlaylistsCommand);
    }

    Pager<List<PropertySet>> postedPlaylistsPager() {
        return postedPlaylistsPager;
    }

    private Observable<List<PropertySet>> postedPlaylists(long beforeTime) {
        return loadPostedPlaylistsCommand
                .with(new ChronologicalQueryParams(PAGE_SIZE, beforeTime))
                .toObservable()
                .doOnNext(requestPlaylistsSyncAction)
                .subscribeOn(scheduler)
                .lift(new OperatorSwitchOnEmptyList<>(updatedPostedPlaylists()));
    }
}
