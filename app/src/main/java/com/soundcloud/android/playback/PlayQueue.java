package com.soundcloud.android.playback;

import static com.soundcloud.java.checks.Preconditions.checkArgument;
import static com.soundcloud.java.checks.Preconditions.checkElementIndex;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.StationTrack;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PlayQueue implements Iterable<PlayQueueItem> {

    public static final String DEFAULT_SOURCE_VERSION = "default";
    private final List<PlayQueueItem> playQueueItems;
    private final Function<PlayQueueItem, Integer> toHashes = new Function<PlayQueueItem, Integer>() {
        @Override
        public Integer apply(PlayQueueItem playQueueItem) {
            return playQueueItem.hashCode();
        }
    };

    public static PlayQueue empty() {
        return new PlayQueue(Collections.<PlayQueueItem>emptyList());
    }

    public static PlayQueue fromTrackUrnList(List<Urn> trackUrns, PlaySessionSource playSessionSource) {
        return new PlayQueue(playQueueItemsFromIds(trackUrns, playSessionSource));
    }

    public static PlayQueue fromTrackList(List<PropertySet> tracks, PlaySessionSource playSessionSource) {
        return new PlayQueue(playQueueItemsFromTracks(tracks, playSessionSource));
    }

    public PlayQueue copy() {
        return new PlayQueue(new ArrayList<>(playQueueItems));
    }

    public PlayQueue(List<PlayQueueItem> playQueueItems) {
        this.playQueueItems = playQueueItems;
    }

    public PlayQueueItem getPlayQueueItem(int position) {
        checkElementIndex(position, size());
        return playQueueItems.get(position);
    }

    public void insertAudioAd(int position, Urn trackUrn, AudioAd adData, boolean shouldPersist) {
        insertPlayQueueItem(position, new TrackQueueItem.Builder(trackUrn)
                .withAdData(adData)
                .persist(shouldPersist)
                .build());
    }

    public void insertVideo(int position, VideoAd videoAd) {
        insertPlayQueueItem(position, new VideoQueueItem(videoAd));
    }

    public boolean hasPreviousItem(int position) {
        return position > 0 && !playQueueItems.isEmpty();
    }

    public boolean hasNextItem(int position) {
        return position < playQueueItems.size() - 1;
    }

    @Override
    public Iterator<PlayQueueItem> iterator() {
        return playQueueItems.iterator();
    }

    public int size() {
        return playQueueItems.size();
    }

    public boolean isEmpty() {
        return playQueueItems.isEmpty();
    }

    public boolean hasItems() {
        return !playQueueItems.isEmpty();
    }

    @Deprecated
    public Urn getUrn(int position) {
        checkElementIndex(position, size());

        final PlayQueueItem playQueueItem = playQueueItems.get(position);
        return playQueueItem.isTrack() ?
                playQueueItem.getUrn() : Urn.NOT_SET;
    }

    public int indexOfTrackUrn(final Urn trackUrn) {
        return Iterables.indexOf(playQueueItems, isMatchingItem(trackUrn));
    }

    public int indexOfTrackUrn(int startPosition, final Urn urn) {
        final List<PlayQueueItem> subList = playQueueItems.subList(startPosition, this.playQueueItems.size());
        final int index = Iterables.indexOf(subList, isMatchingItem(urn));
        if (index >= 0) {
            return index + startPosition;
        } else {
            return index;
        }
    }

    private Predicate<PlayQueueItem> isMatchingItem(final Urn urn) {
        return new Predicate<PlayQueueItem>() {
            @Override
            public boolean apply(PlayQueueItem input) {
                return input.isTrack() && input.getUrn().equals(urn);
            }
        };
    }

    public List<Urn> getTrackItemUrns() {
        final List<Urn> trackItemUrns = new ArrayList<>();
        for (PlayQueueItem item : playQueueItems) {
            if (item.isTrack()) {
                trackItemUrns.add(item.getUrn());
            }
        }
        return trackItemUrns;
    }

    public List<Urn> getItemUrnsFromPosition(int position) {
        final List<Urn> itemUrns = new ArrayList<>(Math.max(0, size() - position));
        for (int i = position; i < playQueueItems.size(); i++) {
            final PlayQueueItem item = getPlayQueueItem(i);
            if (item.isTrack()){
                itemUrns.add(item.getUrn());
            } else {
                itemUrns.add(Urn.NOT_SET);
            }
        }
        return itemUrns;
    }

    public boolean shouldPersistItemAt(int position) {
        return position >= 0 && position < playQueueItems.size() && playQueueItems.get(position).shouldPersist();
    }

    public Optional<AdData> getAdData(int position) {
        checkElementIndex(position, size());
        return playQueueItems.get(position).getAdData();
    }

    public void setAdData(int position, Optional<AdData> adData) {
        checkElementIndex(position, size());
        playQueueItems.get(position).setAdData(adData);
    }

    public static PlayQueue shuffled(List<Urn> tracks, PlaySessionSource playSessionSource) {
        List<Urn> shuffled = newArrayList(tracks);
        Collections.shuffle(shuffled);
        return fromTrackUrnList(shuffled, playSessionSource);
    }

    public static PlayQueue fromStation(Urn stationUrn, List<StationTrack> stationTracks) {
        List<PlayQueueItem> playQueueItems = new ArrayList<>();
        for (StationTrack stationTrack : stationTracks) {
            final TrackQueueItem.Builder builder = new TrackQueueItem.Builder(stationTrack.getTrackUrn())
                    .relatedEntity(stationUrn)
                    .fromSource(
                            PlaySessionSource.DiscoverySource.STATIONS.value(),
                            DEFAULT_SOURCE_VERSION,
                            stationUrn,
                            stationTrack.getQueryUrn()
                    );
            playQueueItems.add(builder.build());
        }
        return new PlayQueue(playQueueItems);
    }

    public static PlayQueue fromRecommendations(Urn seedTrack, RecommendedTracksCollection relatedTracks) {
        List<PlayQueueItem> playQueueItems = new ArrayList<>();
        for (ApiTrack relatedTrack : relatedTracks) {
            final TrackQueueItem.Builder builder = new TrackQueueItem.Builder(relatedTrack.getUrn())
                    .relatedEntity(seedTrack)
                    .fromSource(PlaySessionSource.DiscoverySource.RECOMMENDER.value(), relatedTracks.getSourceVersion());
            playQueueItems.add(builder.build());
        }
        return new PlayQueue(playQueueItems);
    }

    public static PlayQueue fromRecommendationsWithPrependedSeed(Urn seedTrack, RecommendedTracksCollection relatedTracks) {
        PlayQueue playQueue = fromRecommendations(seedTrack, relatedTracks);
        playQueue.playQueueItems.add(0, new TrackQueueItem.Builder(seedTrack).build());
        return playQueue;
    }

    void insertPlayQueueItem(int position, PlayQueueItem playQueueItem) {
        checkArgument(position >= 0 && position <= size(), String.format("Cannot insert item at position:%d, size:%d", position, playQueueItems.size()));
        playQueueItems.add(position, playQueueItem);
    }

    void addPlayQueueItem(PlayQueueItem playQueueItem) {
        playQueueItems.add(playQueueItem);
    }

    void addAllPlayQueueItems(Iterable<PlayQueueItem> somePlayQueueItems) {
        Iterables.addAll(playQueueItems, somePlayQueueItems);
    }

    List<Integer> getQueueHashes() {
        return transform(playQueueItems, toHashes);
    }

    private static List<PlayQueueItem> playQueueItemsFromIds(List<Urn> trackIds,
                                                             final PlaySessionSource playSessionSource) {
        return newArrayList(transform(trackIds, new Function<Urn, PlayQueueItem>() {
            @Override
            public PlayQueueItem apply(Urn track) {
                return new TrackQueueItem.Builder(track)
                        .fromSource(playSessionSource.getInitialSource(), playSessionSource.getInitialSourceVersion())
                        .build();
            }
        }));
    }

    private static List<PlayQueueItem> playQueueItemsFromTracks(List<PropertySet> trackIds, final PlaySessionSource playSessionSource) {
        return newArrayList(transform(trackIds, new Function<PropertySet, PlayQueueItem>() {
            @Override
            public PlayQueueItem apply(PropertySet track) {
                return new TrackQueueItem.Builder(track)
                        .fromSource(playSessionSource.getInitialSource(), playSessionSource.getInitialSourceVersion()).build();
            }
        }));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PlayQueue playQueue = (PlayQueue) o;
        return MoreObjects.equal(playQueueItems, playQueue.playQueueItems);
    }

    @Override
    public int hashCode() {
        return playQueueItems.hashCode();
    }
}
