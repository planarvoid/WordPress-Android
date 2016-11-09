package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.AddTrackToPlaylistCommand.AddTrackToPlaylistParams;
import static com.soundcloud.android.playlists.RemoveTrackFromPlaylistCommand.RemoveTrackFromPlaylistParams;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.EditPlaylistCommand.EditPlaylistCommandParams;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlaylistOperations {

    private static final Func1<List<PlaylistItem>, Map<Urn, PlaylistItem>> TO_URN_MAP = new Func1<List<PlaylistItem>, Map<Urn, PlaylistItem>>() {
        @Override
        public Map<Urn, PlaylistItem> call(List<PlaylistItem> playlistEntities) {
            HashMap<Urn, PlaylistItem> entityMap = new HashMap<>(playlistEntities.size());
            for (PlaylistItem entity : playlistEntities) {
                entityMap.put(entity.getUrn(), entity);
            }
            return entityMap;
        }
    };

    private final Action1<PropertySet> publishTrackAddedToPlaylistEvent = new Action1<PropertySet>() {
        @Override
        public void call(PropertySet newPlaylistTrackData) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                             EntityStateChangedEvent.fromTrackAddedToPlaylist(newPlaylistTrackData));
        }
    };

    private final Action1<PropertySet> publishPlaylistEditedEvent = new Action1<PropertySet>() {
        @Override
        public void call(PropertySet newPlaylistTrackData) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                             EntityStateChangedEvent.fromPlaylistEdited(newPlaylistTrackData));
        }
    };

    private final Action1<PropertySet> publishTrackRemovedFromPlaylistEvent = new Action1<PropertySet>() {
        @Override
        public void call(PropertySet newPlaylistTrackData) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                             EntityStateChangedEvent.fromTrackRemovedFromPlaylist(newPlaylistTrackData));
        }
    };

    private final Action1<Urn> publishPlaylistCreatedEvent = new Action1<Urn>() {
        @Override
        public void call(Urn urn) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromEntityCreated(urn));
        }
    };

    private final Scheduler scheduler;
    private final PlaylistStorage playlistStorage;
    private final Provider<LoadPlaylistTrackUrnsCommand> loadPlaylistTrackUrnsProvider;
    private final PlaylistTracksStorage playlistTracksStorage;
    private final AddTrackToPlaylistCommand addTrackToPlaylistCommand;
    private final RemoveTrackFromPlaylistCommand removeTrackFromPlaylistCommand;
    private final EditPlaylistCommand editPlaylistCommand;
    private final SyncInitiator syncInitiator;
    private final SyncInitiatorBridge syncInitiatorBridge;
    private final EventBus eventBus;

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
                       Provider<LoadPlaylistTrackUrnsCommand> loadPlaylistTrackUrnsProvider,
                       AddTrackToPlaylistCommand addTrackToPlaylistCommand,
                       RemoveTrackFromPlaylistCommand removeTrackFromPlaylistCommand,
                       EditPlaylistCommand editPlaylistCommand,
                       SyncInitiatorBridge syncInitiatorBridge,
                       EventBus eventBus) {
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
        this.playlistTracksStorage = playlistTracksStorage;
        this.playlistStorage = playlistStorage;
        this.loadPlaylistTrackUrnsProvider = loadPlaylistTrackUrnsProvider;
        this.addTrackToPlaylistCommand = addTrackToPlaylistCommand;
        this.removeTrackFromPlaylistCommand = removeTrackFromPlaylistCommand;
        this.editPlaylistCommand = editPlaylistCommand;
        this.syncInitiatorBridge = syncInitiatorBridge;
        this.eventBus = eventBus;
    }

    Observable<List<AddTrackToPlaylistItem>> loadPlaylistForAddingTrack(Urn trackUrn) {
        return playlistTracksStorage
                .loadAddTrackToPlaylistItems(trackUrn)
                .subscribeOn(scheduler);
    }

    Observable<Urn> createNewPlaylist(String title, boolean isPrivate, Urn firstTrackUrn) {
        return playlistTracksStorage.createNewPlaylist(title, isPrivate, firstTrackUrn)
                                    .doOnNext(publishPlaylistCreatedEvent)
                                    .subscribeOn(scheduler)
                                    .doOnCompleted(syncInitiator.requestSystemSyncAction());
    }

    Observable<PropertySet> editPlaylist(Urn playlistUrn, String title, boolean isPrivate, List<Urn> updatedTracklist) {
        return editPlaylistCommand.toObservable(new EditPlaylistCommandParams(playlistUrn,
                                                                              title,
                                                                              isPrivate,
                                                                              updatedTracklist))
                                  .map(toEditedChangeSet(playlistUrn, title, isPrivate))
                                  .doOnNext(publishPlaylistEditedEvent)
                                  .doOnCompleted(syncInitiator.requestSystemSyncAction())
                                  .subscribeOn(scheduler);
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

    private Func1<Integer, PropertySet> toEditedChangeSet(final Urn targetUrn,
                                                          final String title,
                                                          final boolean isPrivate) {
        return new Func1<Integer, PropertySet>() {
            @Override
            public PropertySet call(Integer newTrackCount) {
                return PropertySet.from(
                        PlaylistProperty.URN.bind(targetUrn),
                        PlaylistProperty.TITLE.bind(title),
                        PlaylistProperty.IS_PRIVATE.bind(isPrivate),
                        PlaylistProperty.TRACK_COUNT.bind(newTrackCount));
            }
        };
    }

    public Observable<List<Urn>> trackUrnsForPlayback(final Urn playlistUrn) {
        return loadPlaylistTrackUrnsProvider.get().with(playlistUrn)
                                            .toObservable()
                                            .subscribeOn(scheduler)
                                            .flatMap(new Func1<List<Urn>, Observable<List<Urn>>>() {
                                                @Override
                                                public Observable<List<Urn>> call(List<Urn> trackItems) {
                                                    if (trackItems.isEmpty()) {
                                                        return updatedUrnsForPlayback(playlistUrn);
                                                    } else {
                                                        return Observable.just(trackItems);
                                                    }
                                                }
                                            });
    }

    public Observable<PlaylistWithTracks> playlist(final Urn playlistUrn) {
        return playlistWithTracks(playlistUrn).flatMap(syncIfNecessary(playlistUrn));
    }

    public Observable<List<PlaylistItem>> playlists(final Set<Urn> playlistUrns) {
        return playlistStorage.loadPlaylists(playlistUrns);
    }

    public Observable<Map<Urn, PlaylistItem>> playlistsMap(final Set<Urn> playlistUrns) {
        return playlists(playlistUrns).map(TO_URN_MAP);
    }

    Observable<PlaylistWithTracks> updatedPlaylistInfo(final Urn playlistUrn) {
        return syncInitiator
                .syncPlaylist(playlistUrn)
                .observeOn(scheduler)
                .flatMap(new Func1<SyncJobResult, Observable<PlaylistWithTracks>>() {
                    @Override
                    public Observable<PlaylistWithTracks> call(SyncJobResult playlistWasUpdated) {
                        return playlistWithTracks(playlistUrn)
                                .flatMap(validateLoadedPlaylist);
                    }
                });
    }

    Observable<List<Urn>> updatedUrnsForPlayback(final Urn playlistUrn) {
        return syncInitiator
                .syncPlaylist(playlistUrn)
                .flatMap(new Func1<SyncJobResult, Observable<List<Urn>>>() {
                    @Override
                    public Observable<List<Urn>> call(SyncJobResult syncJobResult) {
                        return loadPlaylistTrackUrnsProvider.get().with(playlistUrn)
                                                            .toObservable()
                                                            .subscribeOn(scheduler);
                    }
                });
    }

    private Observable<PlaylistWithTracks> playlistWithTracks(Urn playlistUrn) {
        return Observable.zip(
                playlistStorage.loadPlaylist(playlistUrn),
                playlistTracksStorage.playlistTracks(playlistUrn).map(TrackItem.fromPropertySets()),
                mergePlaylistWithTracks
        ).subscribeOn(scheduler);
    }

    private Func1<PlaylistWithTracks, Observable<PlaylistWithTracks>> syncIfNecessary(final Urn playlistUrn) {
        return new Func1<PlaylistWithTracks, Observable<PlaylistWithTracks>>() {
            @Override
            public Observable<PlaylistWithTracks> call(PlaylistWithTracks playlistWithTracks) {

                if (playlistWithTracks.isLocalPlaylist()) {
                    syncInitiatorBridge.refreshMyPlaylists();
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
