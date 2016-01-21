package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineState.DOWNLOADED;
import static com.soundcloud.android.offline.OfflineState.DOWNLOADING;
import static com.soundcloud.android.offline.OfflineState.NOT_OFFLINE;
import static com.soundcloud.android.offline.OfflineState.REQUESTED;
import static com.soundcloud.android.offline.OfflineState.UNAVAILABLE;
import static com.soundcloud.java.collections.MoreCollections.filter;
import static com.soundcloud.java.collections.MoreCollections.transform;
import static java.util.Collections.singletonList;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistWithTracks;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class OfflineStatePublisher {

    private static final Predicate<PlaylistWithTracks> FILTER_EMPTY_PLAYLISTS = new Predicate<PlaylistWithTracks>() {
        @Override
        public boolean apply(PlaylistWithTracks input) {
            return input.getTrackCount() == 0;
        }
    };
    private final EventBus eventBus;
    private final OfflineTracksCollectionStateOperations collectionStateOperations;

    @Inject
    public OfflineStatePublisher(EventBus eventBus, OfflineTracksCollectionStateOperations collectionStateOperations) {
        this.eventBus = eventBus;
        this.collectionStateOperations = collectionStateOperations;
    }

    void publishEmptyCollections(ExpectedOfflineContent expectedOfflineContent) {
        final Collection<PlaylistWithTracks> emptyPlaylists = filter(expectedOfflineContent.offlinePlaylists, FILTER_EMPTY_PLAYLISTS);
        final boolean isLikedTracksEmpty = expectedOfflineContent.isLikedTracksExpected && expectedOfflineContent.likedTracks.isEmpty();

        eventBus.publish(
                EventQueue.OFFLINE_CONTENT_CHANGED,
                new OfflineContentChangedEvent(DOWNLOADED,
                        transform(emptyPlaylists, PlaylistWithTracks.TO_URN),
                        isLikedTracksEmpty)
        );
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

    void publishRemoved(Collection<Urn> tracks) {
        publishUpdatesForTrack(NOT_OFFLINE, tracks);
    }

    void publishUnavailable(Collection<Urn> tracks) {
        publishUpdatesForTrack(UNAVAILABLE, tracks);
    }

    void publishCancel(Urn track) {
        publishUpdatesForTrack(REQUESTED, track);
    }

    void publishError(Urn track) {
        publishUpdatesForTrack(REQUESTED, track);
    }

    private void publishUpdatesForTrack(final OfflineState newTrackState, Urn track) {
        publishUpdatesForTrack(newTrackState, singletonList(track));
    }

    private void publishUpdatesForTrack(OfflineState newTracksState, Collection<Urn> tracks) {
        Map<OfflineState, TrackCollections> collectionsStates = new HashMap<>();
        for (Urn track : tracks) {
            collectionsStates = mergeStates(collectionsStates, collectionStateOperations.loadTracksCollectionsState(track, newTracksState));
        }

        for (OfflineState state : OfflineState.values()) {
            final Optional<Collection<Urn>> tracksForState = newTracksState.equals(state)
                    ? Optional.of(tracks)
                    : Optional.<Collection<Urn>>absent();
            final Optional<TrackCollections> collectionsForState = collectionsStates.containsKey(state)
                    ? Optional.of(collectionsStates.get(state))
                    : Optional.<TrackCollections>absent();

            if (tracksForState.isPresent() || collectionsForState.isPresent()) {
                eventBus.publish(
                        EventQueue.OFFLINE_CONTENT_CHANGED,
                        createOfflineContentChangedEvent(state, tracksForState, collectionsForState)
                );
            }
        }
    }

    private static HashMap<OfflineState, TrackCollections> mergeStates(Map<OfflineState, TrackCollections> previousStates, Map<OfflineState, TrackCollections> addedStates) {
        final HashMap<OfflineState, TrackCollections> newStates = new HashMap<>();
        newStates.putAll(previousStates);

        for (Map.Entry<OfflineState, TrackCollections> entry : addedStates.entrySet()) {
            final OfflineState state = entry.getKey();
            final TrackCollections newTrackCollections = entry.getValue();

            if (previousStates.containsKey(state)) {
                final TrackCollections collections = previousStates.get(state);
                final boolean isLikedTracksCollection = collections.likesCollection() || newTrackCollections.likesCollection();
                final List<Urn> mergedPlaylists = new ArrayList<>(collections.playlists().size() + newTrackCollections.playlists().size());
                mergedPlaylists.addAll(collections.playlists());
                mergedPlaylists.addAll(newTrackCollections.playlists());
                newStates.put(state, TrackCollections.create(mergedPlaylists, isLikedTracksCollection));
            } else {
                newStates.put(state, newTrackCollections);
            }
        }
        return newStates;
    }

    private static OfflineContentChangedEvent createOfflineContentChangedEvent(OfflineState state, Optional<Collection<Urn>> track, Optional<TrackCollections> collections) {
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
