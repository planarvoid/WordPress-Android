package com.soundcloud.android.likes;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class LikeOperations {

    private final LoadLikedTracksCommand loadLikedTracksCommand;
    private final LoadLikedPlaylistsCommand loadLikedPlaylistsCommand;
    private final LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;

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
        return loadLikedTracksCommand.toObservable().subscribeOn(scheduler)
                .flatMap(returnIfNonEmptyOr(updatedLikedTracks()));
    }

    public Observable<List<PropertySet>> updatedLikedTracks() {
        return syncInitiator.syncTrackLikes().flatMap(loadLikedTracksCommand);
    }

    public Observable<List<PropertySet>> likedPlaylists() {
        return loadLikedPlaylistsCommand.toObservable().subscribeOn(scheduler)
                .flatMap(returnIfNonEmptyOr(updatedLikedPlaylists()));
    }

    public Observable<List<PropertySet>> updatedLikedPlaylists() {
        return syncInitiator.syncPlaylistLikes().flatMap(loadLikedPlaylistsCommand);
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
}
