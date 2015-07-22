package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.AddTrackToPlaylistCommand.AddTrackToPlaylistParams;
import static com.soundcloud.android.playlists.RemoveTrackFromPlaylistCommand.RemoveTrackFromPlaylistParams;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class PlaylistOperations {

    private final Action1<PropertySet> publishTrackAddedToPlaylistEvent = new Action1<PropertySet>() {
        @Override
        public void call(PropertySet newPlaylistTrackData) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromTrackAddedToPlaylist(newPlaylistTrackData));
        }
    };

    private final Action1<PropertySet> publishTrackRemovedFromPlaylistEvent = new Action1<PropertySet>() {
        @Override
        public void call(PropertySet newPlaylistTrackData) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromTrackRemovedFromPlaylist(newPlaylistTrackData));
        }
    };

    private final Scheduler scheduler;
    private final PlaylistStorage playlistStorage;
    private final LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns;
    private final PlaylistTracksStorage playlistTracksStorage;
    private final AddTrackToPlaylistCommand addTrackToPlaylistCommand;
    private final RemoveTrackFromPlaylistCommand removeTrackFromPlaylistCommand;
    private final SyncInitiator syncInitiator;
    private final EventBus eventBus;
    private final OfflineContentOperations offlineOperations;

    private final Func2<PropertySet, List<TrackItem>, PlaylistWithTracks> mergePlaylistWithTracks =
            new Func2<PropertySet, List<TrackItem>, PlaylistWithTracks>() {
        @Override
        public PlaylistWithTracks call(PropertySet playlist, List<TrackItem> tracks) {
            return new PlaylistWithTracks(playlist, tracks);
        }
    };

    private final Func1<PlaylistWithTracks, Observable<PlaylistWithTracks>> validateLoadedPlaylist = new Func1<PlaylistWithTracks, Observable<PlaylistWithTracks>>() {
        @Override
        public Observable<PlaylistWithTracks> call(PlaylistWithTracks playlistWithTracks) {
            return playlistWithTracks.isMissingMetaData()
                    ? Observable.<PlaylistWithTracks>error(new PlaylistOperations.PlaylistMissingException())
                    : Observable.just(playlistWithTracks);
        }
    };

    @Inject
    PlaylistOperations(@Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                       SyncInitiator syncInitiator,
                       PlaylistTracksStorage playlistTracksStorage,
                       PlaylistStorage playlistStorage,
                       LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns,
                       OfflineContentOperations offlineOperations,
                       AddTrackToPlaylistCommand addTrackToPlaylistCommand,
                       RemoveTrackFromPlaylistCommand removeTrackFromPlaylistCommand,
                       EventBus eventBus) {
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
        this.playlistTracksStorage = playlistTracksStorage;
        this.playlistStorage = playlistStorage;
        this.loadPlaylistTrackUrns = loadPlaylistTrackUrns;
        this.addTrackToPlaylistCommand = addTrackToPlaylistCommand;
        this.removeTrackFromPlaylistCommand = removeTrackFromPlaylistCommand;
        this.eventBus = eventBus;
        this.offlineOperations = offlineOperations;
    }

    Observable<List<AddTrackToPlaylistItem>> loadPlaylistForAddingTrack(Urn trackUrn) {
        return playlistTracksStorage
                .loadAddTrackToPlaylistItems(trackUrn)
                .subscribeOn(scheduler);
    }

    Observable<Urn> createNewPlaylist(String title, boolean isPrivate, Urn firstTrackUrn) {
        return playlistTracksStorage.createNewPlaylist(title, isPrivate, firstTrackUrn)
                .subscribeOn(scheduler)
                .doOnCompleted(syncInitiator.requestSystemSyncAction());
    }

    Observable<Boolean> createNewOfflinePlaylist(String title, boolean isPrivate, Urn firstTrackUrn) {
        return createNewPlaylist(title, isPrivate, firstTrackUrn).flatMap(new Func1<Urn, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Urn urn) {
                return offlineOperations.makePlaylistAvailableOffline(urn);
            }
        });
    }

    Observable<PropertySet> addTrackToPlaylist(Urn playlistUrn, Urn trackUrn) {
        final AddTrackToPlaylistParams params = new AddTrackToPlaylistParams(playlistUrn, trackUrn);
        return addTrackToPlaylistCommand.toObservable(params)
                .map(toChangeSet(playlistUrn))
                .doOnNext(publishTrackAddedToPlaylistEvent)
                .doOnCompleted(syncInitiator.requestSystemSyncAction())
                .subscribeOn(scheduler);
    }

    public Observable<PropertySet> removeTrackFromPlaylist(Urn playlistUrn, Urn trackUrn) {
        final RemoveTrackFromPlaylistParams params = new RemoveTrackFromPlaylistParams(playlistUrn, trackUrn);
        return removeTrackFromPlaylistCommand.toObservable(params)
                .map(toChangeSet(playlistUrn))
                .doOnNext(publishTrackRemovedFromPlaylistEvent)
                .doOnCompleted(syncInitiator.requestSystemSyncAction())
                .subscribeOn(scheduler);
    }

    private Func1<Integer, PropertySet> toChangeSet(final Urn targetUrn) {
        return new Func1<Integer, PropertySet>() {
            @Override
            public PropertySet call(Integer newTrackCount) {
                return PropertySet.from(
                        PlaylistProperty.URN.bind(targetUrn),
                        PlaylistProperty.TRACK_COUNT.bind(newTrackCount));
            }
        };
    }

    public Observable<List<Urn>> trackUrnsForPlayback(Urn playlistUrn) {
        return loadPlaylistTrackUrns.with(playlistUrn)
                .toObservable()
                .subscribeOn(scheduler);
    }

    public Observable<PlaylistWithTracks> playlist(final Urn playlistUrn) {
        final Observable<PlaylistWithTracks> loadObservable = createPlaylistInfoLoadObservable(playlistUrn);
        return loadObservable.flatMap(syncIfNecessary(playlistUrn));
    }

    Observable<PlaylistWithTracks> updatedPlaylistInfo(final Urn playlistUrn) {
        return syncInitiator
                .syncPlaylist(playlistUrn)
                .observeOn(scheduler)
                .flatMap(new Func1<SyncResult, Observable<PlaylistWithTracks>>() {
                    @Override
                    public Observable<PlaylistWithTracks> call(SyncResult playlistWasUpdated) {
                        return createPlaylistInfoLoadObservable(playlistUrn)
                                .flatMap(validateLoadedPlaylist);
                    }
                });
    }

    private Observable<PlaylistWithTracks> createPlaylistInfoLoadObservable(Urn playlistUrn) {
        final Observable<PropertySet> loadPlaylist = playlistStorage.loadPlaylist(playlistUrn);
        final Observable<List<TrackItem>> loadPlaylistTracks =
                playlistTracksStorage.playlistTracks(playlistUrn).map(TrackItem.fromPropertySets());
        return Observable.zip(loadPlaylist, loadPlaylistTracks, mergePlaylistWithTracks).subscribeOn(scheduler);
    }

    private Func1<PlaylistWithTracks, Observable<PlaylistWithTracks>> syncIfNecessary(final Urn playlistUrn) {
        return new Func1<PlaylistWithTracks, Observable<PlaylistWithTracks>>() {
            @Override
            public Observable<PlaylistWithTracks> call(PlaylistWithTracks playlistWithTracks) {

                if (playlistWithTracks.isLocalPlaylist()) {
                    syncInitiator.syncLocalPlaylists();
                    return Observable.just(playlistWithTracks);

                } else if (playlistWithTracks.isMissingMetaData()) {
                    return updatedPlaylistInfo(playlistUrn);

                } else if (playlistWithTracks.needsTracks()) {
                    return Observable.concat(Observable.just(playlistWithTracks), updatedPlaylistInfo(playlistUrn));

                } else {
                    return Observable.just(playlistWithTracks);
                }
            }
        };
    }

    public static class PlaylistMissingException extends Exception {

    }
}
