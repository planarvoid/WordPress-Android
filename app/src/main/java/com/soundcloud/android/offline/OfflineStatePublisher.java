package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineState.DOWNLOADED;
import static com.soundcloud.android.offline.OfflineState.DOWNLOADING;
import static com.soundcloud.android.offline.OfflineState.NOT_OFFLINE;
import static com.soundcloud.android.offline.OfflineState.REQUESTED;
import static com.soundcloud.android.offline.OfflineState.UNAVAILABLE;
import static java.util.Collections.singletonList;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.LambdaObserver;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import io.reactivex.Observable;
import io.reactivex.Single;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

class OfflineStatePublisher {

    private final EventBus eventBus;
    private final OfflineStateOperations collectionStateOperations;

    @Inject
    public OfflineStatePublisher(EventBus eventBus, OfflineStateOperations collectionStateOperations) {
        this.eventBus = eventBus;
        this.collectionStateOperations = collectionStateOperations;
    }

    void publishEmptyCollections(ExpectedOfflineContent expectedOfflineContent) {
        final boolean isLikedTracksEmpty = expectedOfflineContent.isLikedTracksExpected && expectedOfflineContent.likedTracks
                .isEmpty();

        eventBus.publish(
                EventQueue.OFFLINE_CONTENT_CHANGED,
                new OfflineContentChangedEvent(REQUESTED, expectedOfflineContent.emptyPlaylists, isLikedTracksEmpty)
        );
    }

    void publishRequested(Urn track) {
        publishUpdatesForTrack(REQUESTED, track);
    }

    void publishRequested(Collection<Urn> tracks) {
        publishUpdatesForTrack(REQUESTED, tracks);
    }

    void publishDownloading(Urn track) {
        publishUpdatesForTrack(DOWNLOADING, track);
    }

    void publishDownloaded(Collection<Urn> tracks) {
        publishUpdatesForTrack(DOWNLOADED, tracks);
    }

    void publishDownloaded(Urn track) {
        publishUpdatesForTrack(DOWNLOADED, track);
    }

    void publishRemoved(Urn track) {
        publishUpdatesForTrack(NOT_OFFLINE, track);
    }

    void publishRemoved(Collection<Urn> tracks) {
        publishUpdatesForTrack(NOT_OFFLINE, tracks);
    }

    void publishUnavailable(Urn track) {
        publishUpdatesForTrack(UNAVAILABLE, track);
    }

    void publishUnavailable(Collection<Urn> tracks) {
        publishUpdatesForTrack(UNAVAILABLE, tracks);
    }

    private void publishUpdatesForTrack(final OfflineState newTrackState, Urn track) {
        publishUpdatesForTrack(newTrackState, singletonList(track));
    }

    private void publishUpdatesForTrack(OfflineState newTracksState, Collection<Urn> tracks) {
        Single<Map<OfflineState, TrackCollections>> collectionsStates = collectionStateOperations.loadTracksCollectionsState(tracks, newTracksState);
        collectionsStates.flatMapObservable(offlineStateTrackCollectionsMap -> Observable.fromIterable(Arrays.asList(OfflineState.values()))
                                                                                         .filter(state -> newTracksState.equals(state) || offlineStateTrackCollectionsMap.containsKey(state))
                                                                                         .map(state -> toEvent(newTracksState, tracks, offlineStateTrackCollectionsMap, state)))
                         .subscribeWith(LambdaObserver.onNext(event -> eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, event)));
    }

    private OfflineContentChangedEvent toEvent(OfflineState newTracksState, Collection<Urn> tracks, Map<OfflineState, TrackCollections> offlineStateTrackCollectionsMap, OfflineState state) {
        final Optional<Collection<Urn>> tracksForState = newTracksState.equals(state) ? Optional.of(tracks) : Optional.absent();
        final Optional<TrackCollections> collectionsForState = offlineStateTrackCollectionsMap.containsKey(state) ? Optional.of(offlineStateTrackCollectionsMap.get(state)) : Optional.absent();
        return createOfflineContentChangedEvent(state, tracksForState, collectionsForState);
    }

    private static OfflineContentChangedEvent createOfflineContentChangedEvent(OfflineState state,
                                                                               Optional<Collection<Urn>> track,
                                                                               Optional<TrackCollections> collections) {
        final Collection<Urn> entities = new ArrayList<>();
        final boolean isLikedTrackCollection;
        if (collections.isPresent()) {
            entities.addAll(collections.get().playlists());
            isLikedTrackCollection = collections.get().likesCollection();
        } else {
            isLikedTrackCollection = false;
        }

        if (track.isPresent()) {
            entities.addAll(track.get());
        }

        return new OfflineContentChangedEvent(state, entities, isLikedTrackCollection);
    }

}
