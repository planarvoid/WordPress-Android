package com.soundcloud.android.tracks;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class TrackItemRepository {

    private final TrackRepository trackRepository;
    private final EntityItemCreator entityItemCreator;

    @Inject
    public TrackItemRepository(TrackRepository trackRepository,
                               EntityItemCreator entityItemCreator) {
        this.trackRepository = trackRepository;
        this.entityItemCreator = entityItemCreator;
    }

    public Maybe<TrackItem> track(final Urn trackUrn) {
        return trackRepository.track(trackUrn).map(entityItemCreator::trackItem);
    }

    public Single<Map<Urn, TrackItem>> fromUrns(final List<Urn> requestedTracks) {
        return trackRepository.fromUrns(requestedTracks).map(entityItemCreator::convertTrackMap);
    }

    public Single<List<TrackItem>> trackListFromUrns(List<Urn> requestedTracks) {
        return fromUrns(requestedTracks)
                .map(urnTrackMap -> Lists.newArrayList(Iterables.transform(Iterables.filter(requestedTracks, urnTrackMap::containsKey), urnTrackMap::get)));
    }

    public Single<List<TrackItem>> forPlaylist(Urn playlistUrn) {
        return trackRepository.forPlaylist(playlistUrn).map(t -> Lists.transform(t, entityItemCreator::trackItem));

    }

    public Single<List<TrackItem>> forPlaylist(Urn playlistUrn, long staleTimeMillis) {
        return trackRepository.forPlaylist(playlistUrn, staleTimeMillis).map(t -> Lists.transform(t, entityItemCreator::trackItem));
    }

    Observable<TrackItem> fullTrackWithUpdate(final Urn trackUrn) {
        return trackRepository.fullTrackWithUpdate(trackUrn).map(entityItemCreator::trackItem);
    }

}
