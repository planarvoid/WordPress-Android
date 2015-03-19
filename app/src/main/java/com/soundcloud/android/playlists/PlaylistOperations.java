package com.soundcloud.android.playlists;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class PlaylistOperations {

    private final Action1<PropertySet> publishEntityStateChanged = new Action1<PropertySet>() {
        @Override
        public void call(PropertySet newRepostState) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromTrackAddedToPlaylist(newRepostState));
        }
    };

    private final Scheduler storageScheduler;
    private final LoadPlaylistCommand loadPlaylistCommand;
    private final LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns;
    private final LoadPlaylistTracksCommand loadPlaylistTracksCommand;
    private final PlaylistTracksStorage playlistTracksStorage;
    private final SyncInitiator syncInitiator;
    private final EventBus eventBus;
    private final OfflineContentOperations offlineOperations;

    private final Func2<PropertySet, List<PropertySet>, PlaylistInfo> mergePlaylistWithTracks = new Func2<PropertySet, List<PropertySet>, PlaylistInfo>() {
        @Override
        public PlaylistInfo call(PropertySet playlist, List<PropertySet> tracks) {
            return new PlaylistInfo(playlist, tracks);
        }
    };

    @Inject
    PlaylistOperations(@Named("Storage") Scheduler scheduler,
                       SyncInitiator syncInitiator,
                       PlaylistTracksStorage playlistTracksStorage,
                       LoadPlaylistCommand loadPlaylistCommand,
                       LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns,
                       LoadPlaylistTracksCommand loadPlaylistTracksCommand,
                       EventBus eventBus, OfflineContentOperations offlineOperations) {
        this.storageScheduler = scheduler;
        this.syncInitiator = syncInitiator;
        this.loadPlaylistCommand = loadPlaylistCommand;
        this.playlistTracksStorage = playlistTracksStorage;
        this.loadPlaylistTrackUrns = loadPlaylistTrackUrns;
        this.loadPlaylistTracksCommand = loadPlaylistTracksCommand;
        this.eventBus = eventBus;
        this.offlineOperations = offlineOperations;
    }

    Observable<List<PropertySet>> loadPlaylistForAddingTrack(Urn trackUrn) {
        return playlistTracksStorage.playlistsForAddingTrack(trackUrn);
    }

    public Observable<Urn> createNewPlaylist(String title, boolean isPrivate, Urn firstTrackUrn) {
        return playlistTracksStorage.createNewPlaylist(title, isPrivate, firstTrackUrn)
                .subscribeOn(storageScheduler)
                .doOnCompleted(syncInitiator.requestSystemSyncAction());
    }

    public Observable<Boolean> createNewOfflinePlaylist(String title, boolean isPrivate, Urn firstTrackUrn) {
        return createNewPlaylist(title, isPrivate, firstTrackUrn).flatMap(new Func1<Urn, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Urn urn) {
                return offlineOperations.makePlaylistAvailableOffline(urn);
            }
        });
    }

    Observable<PropertySet> addTrackToPlaylist(Urn playlistUrn, Urn trackUrn) {
        return playlistTracksStorage.addTrackToPlaylist(playlistUrn, trackUrn)
                .subscribeOn(storageScheduler)
                .doOnNext(publishEntityStateChanged)
                .doOnCompleted(syncInitiator.requestSystemSyncAction());
    }

    public Observable<List<Urn>> trackUrnsForPlayback(Urn playlistUrn) {
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
