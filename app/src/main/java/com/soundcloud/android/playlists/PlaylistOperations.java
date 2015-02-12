package com.soundcloud.android.playlists;


import static com.google.common.collect.Iterables.getLast;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.likes.ChronologicalQueryParams;
import com.soundcloud.android.model.Urn;
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

class PlaylistOperations {
    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private final LoadPostedPlaylistsCommand loadPostedPlaylistsCommand;
    private final PlaylistStorage playlistStorage;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;
    private final NetworkConnectionHelper networkConnectionHelper;

    private final Func1<Boolean, Observable<List<PropertySet>>> handleSyncResult = new Func1<Boolean, Observable<List<PropertySet>>>() {
        @Override
        public Observable<List<PropertySet>> call(Boolean syncSuccess) {
            return postedPlaylists();
        }
    };

    @Inject
    PlaylistOperations(PlaylistStorage playlistStorage,
                              LoadPostedPlaylistsCommand loadPostedPlaylistsCommand,
                              SyncInitiator syncInitiator,
                              @Named("Storage") Scheduler scheduler,
                              NetworkConnectionHelper networkConnectionHelper) {
        this.playlistStorage = playlistStorage;
        this.loadPostedPlaylistsCommand = loadPostedPlaylistsCommand;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
        this.networkConnectionHelper = networkConnectionHelper;
    }

    public Observable<List<Urn>> trackUrnsForPlayback(Urn playlistUrn) {
        return playlistStorage.trackUrns(playlistUrn).toList();
    }

    private final Func1<SyncResult, ChronologicalQueryParams> toInitalPageParams = new Func1<SyncResult, ChronologicalQueryParams>() {
        @Override
        public ChronologicalQueryParams call(SyncResult syncResult) {
            return new ChronologicalQueryParams(PAGE_SIZE, Long.MAX_VALUE);
        }
    };

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

    public Observable<List<PropertySet>> postedPlaylists() {
        return postedPlaylists(Long.MAX_VALUE);
    }

    public Observable<List<PropertySet>> postedPlaylists(long beforeTime) {
        return loadPostedPlaylistsCommand
                .with(new ChronologicalQueryParams(PAGE_SIZE, beforeTime))
                .toObservable()
                .doOnNext(requestPlaylistsSyncAction)
                .subscribeOn(scheduler);
    }

    public Observable<List<PropertySet>> updatedPostedPlaylists() {
        return syncInitiator.refreshPostedPlaylists().flatMap(handleSyncResult);
    }

    public Pager<List<PropertySet>> postedPlaylistsPager() {
        return postedPlaylistsPager;
    }
}
