package com.soundcloud.android.tracks;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class TrackItemRepository {

    private final TrackRepository trackRepository;
    private final TrackItemCreator trackItemCreator;

    @Inject
    public TrackItemRepository(TrackRepository trackRepository,
                               TrackItemCreator trackItemCreator) {
        this.trackRepository = trackRepository;
        this.trackItemCreator = trackItemCreator;
    }

    public Observable<TrackItem> track(final Urn trackUrn) {
        return trackRepository.track(trackUrn).map(trackItemCreator::trackItem);
    }

    public Observable<Map<Urn, TrackItem>> fromUrns(final List<Urn> requestedTracks) {
        return trackRepository.fromUrns(requestedTracks).map(trackItemCreator::convertMap);
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
        return trackRepository.fullTrackWithUpdate(trackUrn).map(trackItemCreator::trackItem);
    }

    private Observable.Transformer<? super List<Track>, List<TrackItem>> tracksToItems() {
        return tracks -> tracks.flatMap(t -> Observable.from(t).map(trackItemCreator::trackItem).toList());
    }

}
