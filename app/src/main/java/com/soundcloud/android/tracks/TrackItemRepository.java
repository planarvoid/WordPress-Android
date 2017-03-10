package com.soundcloud.android.tracks;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import rx.Observable;

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

    public Observable<TrackItem> track(final Urn trackUrn) {
        return trackRepository.track(trackUrn).map(entityItemCreator::trackItem);
    }

    public Observable<Map<Urn, TrackItem>> fromUrns(final List<Urn> requestedTracks) {
        return trackRepository.fromUrns(requestedTracks).map(entityItemCreator::convertTrackMap);
    }

    public Observable<List<TrackItem>> trackListFromUrns(List<Urn> requestedTracks) {
        return fromUrns(requestedTracks)
                .map(urnTrackMap -> Lists.newArrayList(Iterables.transform(Iterables.filter(requestedTracks, urnTrackMap::containsKey), urnTrackMap::get)));
    }

    public Observable<List<TrackItem>> forPlaylist(Urn playlistUrn) {
        return trackRepository.forPlaylist(playlistUrn)
                              .compose(tracksToItems());

    }

    public Observable<List<TrackItem>> forPlaylist(Urn playlistUrn, long staleTimeMillis) {
        return trackRepository.forPlaylist(playlistUrn, staleTimeMillis)
                              .compose(tracksToItems());

    }

    Observable<TrackItem> fullTrackWithUpdate(final Urn trackUrn) {
        return trackRepository.fullTrackWithUpdate(trackUrn).map(entityItemCreator::trackItem);
    }

    private Observable.Transformer<? super List<Track>, List<TrackItem>> tracksToItems() {
        return tracks -> tracks.flatMap(t -> Observable.from(t).map(entityItemCreator::trackItem).toList());
    }

}
