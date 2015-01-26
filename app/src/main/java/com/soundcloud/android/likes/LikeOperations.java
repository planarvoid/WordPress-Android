package com.soundcloud.android.likes;

import static com.google.common.collect.Iterables.getLast;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.android.Pager;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class LikeOperations {

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private final LoadLikedTracksCommand loadLikedTracksCommand;
    private final LoadLikedPlaylistsCommand loadLikedPlaylistsCommand;
    private final LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;

    private final Func1<SyncResult, ChronologicalQueryParams> toInitalPageParams = new Func1<SyncResult, ChronologicalQueryParams>() {
        @Override
        public ChronologicalQueryParams call(SyncResult syncResult) {
            return new ChronologicalQueryParams(PAGE_SIZE, Long.MAX_VALUE);
        }
    };

    private final Pager<List<PropertySet>> likedTracksPager = new Pager<List<PropertySet>>() {
        @Override
        public Observable<List<PropertySet>> call(List<PropertySet> result) {
            if (result.size() < PAGE_SIZE) {
                return Pager.finish();
            } else {
                return likedTracks(getLast(result).get(LikeProperty.CREATED_AT).getTime());
            }
        }
    };

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

    @Inject
    public LikeOperations(LoadLikedTracksCommand loadLikedTracksCommand,
                          LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand,
                          LoadLikedPlaylistsCommand loadLikedPlaylistsCommand,
                          SyncInitiator syncInitiator,
                          @Named("Storage") Scheduler scheduler) {
        this.loadLikedTracksCommand = loadLikedTracksCommand;
        this.loadLikedPlaylistsCommand = loadLikedPlaylistsCommand;
        this.loadLikedTrackUrnsCommand = loadLikedTrackUrnsCommand;
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
    }

    public Observable<List<PropertySet>> likedTracks() {
        return likedTracks(Long.MAX_VALUE);
    }

    private Observable<List<PropertySet>> likedTracks(long beforeTime) {
        return loadLikedTracksCommand
                .with(new ChronologicalQueryParams(PAGE_SIZE, beforeTime))
                .toObservable()
                .flatMap(returnIfNonEmptyOr(updatedLikedTracks()));
    }

    public Observable<List<PropertySet>> updatedLikedTracks() {
        return syncInitiator.syncTrackLikes().map(toInitalPageParams).flatMap(loadLikedTracksCommand);
    }

    public Observable<List<PropertySet>> likedPlaylists() {
        return likedPlaylists(Long.MAX_VALUE);
    }

    public Observable<List<PropertySet>> likedPlaylists(long beforeTime) {
        return loadLikedPlaylistsCommand
                .with(new ChronologicalQueryParams(PAGE_SIZE, beforeTime))
                .toObservable().subscribeOn(scheduler)
                .flatMap(returnIfNonEmptyOr(updatedLikedPlaylists()));
    }

    public Observable<List<PropertySet>> updatedLikedPlaylists() {
        return syncInitiator.syncPlaylistLikes().map(toInitalPageParams).flatMap(loadLikedPlaylistsCommand);
    }

    private <CollT extends List> Func1<CollT, Observable<CollT>> returnIfNonEmptyOr(final Observable<CollT> syncAndLoadObservable) {
        return new Func1<CollT, Observable<CollT>>() {
            @Override
            public Observable<CollT> call(CollT result) {
                if (result.isEmpty()) {
                    return syncAndLoadObservable;
                } else {
                    return Observable.just(result);
                }
            }
        };
    }

    public Observable<List<Urn>> likedTrackUrns() {
        return loadLikedTrackUrnsCommand.toObservable().subscribeOn(scheduler);
    }

    public Pager<List<PropertySet>> likedTracksPager() {
        return likedTracksPager;
    }

    public Pager<List<PropertySet>> likedPlaylistsPager() {
        return likedPlaylistsPager;
    }
}
