package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.AddTrackToPlaylistCommand.AddTrackToPlaylistParams;
import static com.soundcloud.android.playlists.RemoveTrackFromPlaylistCommand.RemoveTrackFromPlaylistParams;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.Lists.transform;
import static rx.Observable.concat;
import static rx.Observable.just;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.collection.playlists.MyPlaylistsOperations;
import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playlists.EditPlaylistCommand.EditPlaylistCommandParams;
import com.soundcloud.android.profile.ProfileApiMobile;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.Collections;
import java.util.List;

public class PlaylistOperations {

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
    private final Provider<LoadPlaylistTrackUrnsCommand> loadPlaylistTrackUrnsProvider;
    private final PlaylistRepository playlistRepository;
    private final PlaylistTracksStorage playlistTracksStorage;
    private final TrackRepository tracks;
    private final AddTrackToPlaylistCommand addTrackToPlaylistCommand;
    private final RemoveTrackFromPlaylistCommand removeTrackFromPlaylistCommand;
    private final EditPlaylistCommand editPlaylistCommand;
    private final SyncInitiator syncInitiator;
    private final SyncInitiatorBridge syncInitiatorBridge;
    private final OfflineContentOperations offlineContentOperations;
    private final EventBus eventBus;
    private final ProfileApiMobile profileApiMobile;
    private final MyPlaylistsOperations myPlaylistsOperations;
    private final AccountOperations accountOperations;
    private final PlaylistUpsellOperations upsellOperations;
    private final FeatureFlags featureFlags;

    @Inject
    PlaylistOperations(@Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                       SyncInitiator syncInitiator,
                       PlaylistRepository playlistRepository,
                       Provider<LoadPlaylistTrackUrnsCommand> loadPlaylistTrackUrnsProvider,
                       PlaylistTracksStorage playlistTracksStorage,
                       TrackRepository tracks,
                       AddTrackToPlaylistCommand addTrackToPlaylistCommand,
                       RemoveTrackFromPlaylistCommand removeTrackFromPlaylistCommand,
                       EditPlaylistCommand editPlaylistCommand,
                       SyncInitiatorBridge syncInitiatorBridge,
                       OfflineContentOperations offlineContentOperations,
                       EventBus eventBus,
                       ProfileApiMobile profileApiMobile,
                       MyPlaylistsOperations myPlaylistsOperations,
                       AccountOperations accountOperations, PlaylistUpsellOperations upsellOperations,
                       FeatureFlags featureFlags) {
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
        this.playlistRepository = playlistRepository;
        this.loadPlaylistTrackUrnsProvider = loadPlaylistTrackUrnsProvider;
        this.playlistTracksStorage = playlistTracksStorage;
        this.tracks = tracks;
        this.addTrackToPlaylistCommand = addTrackToPlaylistCommand;
        this.removeTrackFromPlaylistCommand = removeTrackFromPlaylistCommand;
        this.editPlaylistCommand = editPlaylistCommand;
        this.syncInitiatorBridge = syncInitiatorBridge;
        this.offlineContentOperations = offlineContentOperations;
        this.eventBus = eventBus;
        this.profileApiMobile = profileApiMobile;
        this.myPlaylistsOperations = myPlaylistsOperations;
        this.accountOperations = accountOperations;
        this.upsellOperations = upsellOperations;
        this.featureFlags = featureFlags;
    }

    Observable<List<AddTrackToPlaylistItem>> loadPlaylistForAddingTrack(Urn trackUrn) {
        return playlistTracksStorage
                .loadAddTrackToPlaylistItems(trackUrn)
                .subscribeOn(scheduler);
    }

    Observable<Urn> createNewPlaylist(String title, boolean isPrivate, boolean isOffline, Urn firstTrackUrn) {
        return playlistTracksStorage.createNewPlaylist(title, isPrivate, firstTrackUrn)
                                    .flatMap(urn -> isOffline ?
                                                    offlineContentOperations.makePlaylistAvailableOffline(urn)
                                                                            .map(aVoid -> urn) :
                                                    Observable.just(urn))
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
        return newTrackCount -> PropertySet.from(
                PlaylistProperty.URN.bind(targetUrn),
                PlaylistProperty.TRACK_COUNT.bind(newTrackCount));
    }

    private Func1<Integer, PropertySet> toEditedChangeSet(final Urn targetUrn,
                                                          final String title,
                                                          final boolean isPrivate) {
        return newTrackCount -> PropertySet.from(
                PlaylistProperty.URN.bind(targetUrn),
                PlaylistProperty.TITLE.bind(title),
                PlaylistProperty.IS_PRIVATE.bind(isPrivate),
                PlaylistProperty.TRACK_COUNT.bind(newTrackCount));
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
                                                        return just(trackItems);
                                                    }
                                                }
                                            });
    }

    public Observable<PlaylistWithTracks> playlist(final Urn playlistUrn) {
        return playlistWithTracks(playlistUrn)
                .flatMap(syncIfNecessary(playlistUrn))
                .switchIfEmpty(updatedPlaylist(playlistUrn));
    }

    private Observable<PlaylistWithTracks> updatedPlaylist(final Urn playlistUrn) {
        return syncInitiator
                .syncPlaylist(playlistUrn)
                .observeOn(scheduler)
                .flatMap(new Func1<SyncJobResult, Observable<PlaylistWithTracks>>() {
                    @Override
                    public Observable<PlaylistWithTracks> call(SyncJobResult playlistWasUpdated) {
                        return playlistWithTracks(playlistUrn)
                                .switchIfEmpty(Observable.error(new PlaylistMissingException()));
                    }
                });
    }

    Observable<PlaylistDetailsViewModel> playlistWithTracksAndRecommendations(Urn playlistUrn,
                                                                              boolean addInlineHeader) {
        return playlist(playlistUrn).flatMap(addOtherPlaylists(addInlineHeader));
    }

    Observable<PlaylistDetailsViewModel> updatedPlaylistWithTracksAndRecommendations(Urn playlistUrn,
                                                                                     boolean addInlineHeader) {
        return updatedPlaylist(playlistUrn).flatMap(addOtherPlaylists(addInlineHeader));
    }

    @NonNull
    private Func1<PlaylistWithTracks, Observable<PlaylistDetailsViewModel>> addOtherPlaylists(final boolean addInlineHeader) {
        return playlistWithTracks -> {
            if (playlistWithTracks.needsTracks() || featureFlags.isDisabled(Flag.OTHER_PLAYLISTS_BY_CREATOR)) {
                return just(Collections.<PlaylistItem>emptyList())
                        .map(toViewModel(addInlineHeader, playlistWithTracks));

            } else if (accountOperations.isLoggedInUser(playlistWithTracks.getCreatorUrn())) {
                return myOtherPlaylists()
                        .map(playlistsWithExclusion(playlistWithTracks))
                        .map(toViewModel(addInlineHeader, playlistWithTracks));
            } else {

                Observable<PlaylistDetailsViewModel> withoutOtherPlaylists = just(Collections.<PlaylistItem>emptyList())
                        .map(toViewModel(addInlineHeader, playlistWithTracks));
                Observable<PlaylistDetailsViewModel> withOtherPlaylists = playlistsForOtherUser(playlistWithTracks.getCreatorUrn())
                        .map(playlistsWithExclusion(playlistWithTracks))
                        .map(toViewModel(addInlineHeader, playlistWithTracks));

                return concat(withoutOtherPlaylists, withOtherPlaylists);
            }
        };
    }

    @NonNull
    private Func1<List<PlaylistItem>, List<PlaylistItem>> playlistsWithExclusion(PlaylistWithTracks playlistWithTracks) {
        return playlistItems -> newArrayList(filter(playlistItems,
                                                    input -> !input.getUrn().equals(playlistWithTracks.getUrn())));
    }

    @NonNull
    private Func1<List<PlaylistItem>, PlaylistDetailsViewModel> toViewModel(boolean addInlineHeader,
                                                                            PlaylistWithTracks playlistWithTracks) {
        return playlistItems -> {
            List<PlaylistDetailItem> playlistDetailItems = upsellOperations.toListItems(playlistWithTracks);
            if (addInlineHeader) {
                playlistDetailItems.add(0, new PlaylistDetailHeaderItem());
            }
            if (!playlistItems.isEmpty()) {
                playlistDetailItems.add(new PlaylistDetailOtherPlaylistsItem(playlistWithTracks.getCreatorName(),
                                                                             playlistItems));
            }
            return new PlaylistDetailsViewModel(playlistWithTracks, playlistDetailItems);
        };
    }

    @NonNull
    private Observable<List<PlaylistItem>> playlistsForOtherUser(Urn userUrn) {
        return profileApiMobile.userPlaylists(userUrn)
                               .map(apiPlaylistPosts -> transform(apiPlaylistPosts.getCollection(),
                                                                  input -> PlaylistItem.from(input.getApiPlaylist())));
    }

    private Observable<List<PlaylistItem>> myOtherPlaylists() {
        return myPlaylistsOperations.myPlaylists(PlaylistsOptions.builder().showLikes(false).showPosts(true).build());
    }

    private Observable<List<Urn>> updatedUrnsForPlayback(final Urn playlistUrn) {
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
                playlistRepository.withUrn(playlistUrn),
                tracks.forPlaylist(playlistUrn),
                PlaylistWithTracks::new
        ).subscribeOn(scheduler);
    }

    private Func1<PlaylistWithTracks, Observable<PlaylistWithTracks>> syncIfNecessary(final Urn playlistUrn) {
        return playlistWithTracks -> {

            if (playlistWithTracks.isLocalPlaylist()) {
                fireAndForget(syncInitiatorBridge.refreshMyPlaylists());
                return just(playlistWithTracks);

            } else if (playlistWithTracks.needsTracks()) {
                return concat(just(playlistWithTracks), updatedPlaylist(playlistUrn));

            } else {
                return just(playlistWithTracks);
            }
        };
    }

    public static class PlaylistMissingException extends Exception {

    }
}
