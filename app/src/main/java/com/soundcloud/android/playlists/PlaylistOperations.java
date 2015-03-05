package com.soundcloud.android.playlists;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

class PlaylistOperations implements PlaylistCreator<PropertySet> {

    private final Scheduler storageScheduler;
    private final LoadPlaylistCommand loadPlaylistCommand;
    private final LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns;
    private final LoadPlaylistTracksCommand loadPlaylistTracksCommand;
    private final SyncInitiator syncInitiator;

    private final Func2<PropertySet, List<PropertySet>, PlaylistInfo> mergePlaylistWithTracks = new Func2<PropertySet, List<PropertySet>, PlaylistInfo>() {
        @Override
        public PlaylistInfo call(PropertySet playlist, List<PropertySet> tracks) {
            return new PlaylistInfo(playlist, tracks);
        }
    };

    @Inject
    PlaylistOperations(@Named("Storage") Scheduler scheduler,
                       SyncInitiator syncInitiator, LoadPlaylistCommand loadPlaylistCommand,
                       LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns,
                       LoadPlaylistTracksCommand loadPlaylistTracksCommand) {

        this.storageScheduler = scheduler;
        this.syncInitiator = syncInitiator;
        this.loadPlaylistCommand = loadPlaylistCommand;
        this.loadPlaylistTrackUrns = loadPlaylistTrackUrns;
        this.loadPlaylistTracksCommand = loadPlaylistTracksCommand;
    }

    @Override
    public Observable<PropertySet> createNewPlaylist(String title, boolean isPrivate, Urn firstTrackUrn) {
        //TODO
        return Observable.just(PropertySet.create());
    }

    Observable<List<Urn>> trackUrnsForPlayback(Urn playlistUrn) {
        return loadPlaylistTrackUrns.with(playlistUrn)
                .toObservable()
                .subscribeOn(storageScheduler);
    }

    Observable<PlaylistInfo> playlistInfo(final Urn playlistUrn) {
        final Observable<PlaylistInfo> loadObservable = createPlaylistInfoLoadObservable(playlistUrn);
        return loadObservable.flatMap(syncIfNecessary(playlistUrn));
    }

    Observable<PlaylistInfo> updatedPlaylistInfo(final Urn playlistUrn) {
        return syncInitiator.syncPlaylist(playlistUrn).flatMap(new Func1<Boolean, Observable<PlaylistInfo>>() {
            @Override
            public Observable<PlaylistInfo> call(Boolean playlistWasUpdated) {
                return createPlaylistInfoLoadObservable(playlistUrn);
            }
        });
    }

    private Observable<PlaylistInfo> createPlaylistInfoLoadObservable(Urn playlistUrn) {
        final Observable<PropertySet> loadPlaylist = loadPlaylistCommand.with(playlistUrn).toObservable();
        final Observable<List<PropertySet>> loadPlaylistTracks = loadPlaylistTracksCommand.with(playlistUrn).toObservable();
        return Observable.zip(loadPlaylist, loadPlaylistTracks, mergePlaylistWithTracks).subscribeOn(storageScheduler);
    }

    private Func1<PlaylistInfo, Observable<PlaylistInfo>> syncIfNecessary(final Urn playlistUrn) {
        return new Func1<PlaylistInfo, Observable<PlaylistInfo>>() {
            @Override
            public Observable<PlaylistInfo> call(PlaylistInfo playlistInfo) {

                if (playlistInfo.isLocalPlaylist()){
                    syncInitiator.syncLocalPlaylists();
                    return Observable.just(playlistInfo);

                } else if (playlistInfo.isMissingMetaData()) {
                    return updatedPlaylistInfo(playlistUrn);

                } else if (playlistInfo.needsTracks()) {
                    return Observable.concat(Observable.just(playlistInfo), updatedPlaylistInfo(playlistUrn));

                } else {
                    return Observable.just(playlistInfo);
                }
            }
        };
    }

}
