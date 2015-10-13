package com.soundcloud.android.playback;

import static com.soundcloud.java.checks.Preconditions.checkArgument;
import static com.soundcloud.java.checks.Preconditions.checkElementIndex;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.objects.MoreObjects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PlayQueue implements Iterable<PlayQueueItem> {

    public static final String DEFAULT_SOURCE_VERSION = "default";
    private final List<PlayQueueItem> playQueueItems;
    private final Function<PlayQueueItem, Urn> toUrn = new Function<PlayQueueItem, Urn>() {
        @Override
        public Urn apply(PlayQueueItem playQueueItem) {
            return playQueueItem.getTrackUrn();
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

    public void addTrack(Urn trackUrn, String source, String sourceVersion) {
        playQueueItems.add(new PlayQueueItem.Builder(trackUrn)
                .fromSource(source, sourceVersion)
                .build());
    }

    public void insertTrack(int position, Urn trackUrn, PropertySet metaData, boolean shouldPersist) {
        checkArgument(position >= 0 && position <= size(), String.format("Cannot insert track at position:%d, size:%d", position, playQueueItems.size()));
        playQueueItems.add(position, new PlayQueueItem.Builder(trackUrn)
                .withAdData(metaData)
                .persist(shouldPersist)
                .build());
    }

    public void remove(int position) {
        playQueueItems.remove(position);
    }

    public boolean hasPreviousTrack(int position) {
        return position > 0 && !playQueueItems.isEmpty();
    }

    public boolean hasNextTrack(int position) {
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

    public Urn getUrn(int position) {
        return position >= 0 && position < size() ? playQueueItems.get(position).getTrackUrn() : Urn.NOT_SET;
    }

    public int indexOf(Urn initialTrack) {
        return transform(playQueueItems, toUrn).indexOf(initialTrack);
    }

    @Deprecated
    public long getTrackId(int position) {
        return position >= 0 && position < size() ? playQueueItems.get(position).getTrackUrn().getNumericId() : Consts.NOT_SET;
    }

    public PropertySet getMetaData(int position) {
        checkElementIndex(position, size());

        return playQueueItems.get(position).getMetaData();
    }

    public boolean shouldPersistTrackAt(int position) {
        return position >= 0 && position < playQueueItems.size() && playQueueItems.get(position).shouldPersist();
    }

    public void setMetaData(int position, PropertySet metadata) {
        checkElementIndex(position, size());

        playQueueItems.get(position).setMetaData(metadata);
    }

    public static PlayQueue shuffled(List<Urn> tracks, PlaySessionSource playSessionSource) {
        List<Urn> shuffled = newArrayList(tracks);
        Collections.shuffle(shuffled);
        return fromTrackUrnList(shuffled, playSessionSource);
    }

    public static PlayQueue fromStation(Urn stationUrn, List<Urn> tracks) {
        List<PlayQueueItem> playQueueItems = new ArrayList<>();
        for (Urn track : tracks) {
            final PlayQueueItem.Builder builder = new PlayQueueItem.Builder(track)
                    .relatedEntity(stationUrn)
                    .fromSource(PlaySessionSource.DiscoverySource.STATIONS.value(), DEFAULT_SOURCE_VERSION);
            playQueueItems.add(builder.build());
        }
        return new PlayQueue(playQueueItems);
    }

    public static PlayQueue fromRecommendations(Urn seedTrack, RecommendedTracksCollection relatedTracks) {
        List<PlayQueueItem> playQueueItems = new ArrayList<>();
        for (ApiTrack relatedTrack : relatedTracks) {
            final PlayQueueItem.Builder builder = new PlayQueueItem.Builder(relatedTrack.getUrn())
                    .relatedEntity(seedTrack)
                    .fromSource(PlaySessionSource.DiscoverySource.RECOMMENDER.value(), relatedTracks.getSourceVersion());
            playQueueItems.add(builder.build());
        }
        return new PlayQueue(playQueueItems);
    }

    public static PlayQueue fromRecommendationsWithPrependedSeed(Urn seedTrack, RecommendedTracksCollection relatedTracks) {
        PlayQueue playQueue = fromRecommendations(seedTrack, relatedTracks);
        playQueue.playQueueItems.add(0, new PlayQueueItem.Builder(seedTrack).build());
        return playQueue;
    }

    void addPlayQueueItem(PlayQueueItem playQueueItem) {
        playQueueItems.add(playQueueItem);
    }

    void addAllPlayQueueItems(Iterable<PlayQueueItem> somePlayQueueItems) {
        Iterables.addAll(playQueueItems, somePlayQueueItems);
    }

    Urn getReposter(int position) {
        return playQueueItems.get(position).getReposter();
    }

    String getTrackSource(int position) {
        return playQueueItems.get(position).getSource();
    }

    String getSourceVersion(int position) {
        return playQueueItems.get(position).getSourceVersion();
    }

    Urn getRelatedEntity(int position) {
        return playQueueItems.get(position).getRelatedEntity();
    }

    List<Long> getTrackIds() {
        List<Long> trackIds = transform(playQueueItems, new Function<PlayQueueItem, Long>() {
            @Override
            public Long apply(PlayQueueItem input) {
                return input.getTrackUrn().getNumericId();
            }
        });
        return trackIds;
    }

    List<Urn> getTrackUrns() {
        return transform(playQueueItems, toUrn);
    }

    private static List<PlayQueueItem> playQueueItemsFromIds(List<Urn> trackIds,
                                                             final PlaySessionSource playSessionSource) {


        return newArrayList(transform(trackIds, new Function<Urn, PlayQueueItem>() {
            @Override
            public PlayQueueItem apply(Urn track) {
                return new PlayQueueItem.Builder(track)
                        .fromSource(playSessionSource.getInitialSource(), playSessionSource.getInitialSourceVersion())
                        .build();
            }
        }));
    }

    private static List<PlayQueueItem> playQueueItemsFromTracks(List<PropertySet> trackIds, final PlaySessionSource playSessionSource) {
        return newArrayList(transform(trackIds, new Function<PropertySet, PlayQueueItem>() {
            @Override
            public PlayQueueItem apply(PropertySet track) {
                return new PlayQueueItem.Builder(track)
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
