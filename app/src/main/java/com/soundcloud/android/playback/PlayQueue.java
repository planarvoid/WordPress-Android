package com.soundcloud.android.playback;

import static com.soundcloud.java.checks.Preconditions.checkArgument;
import static com.soundcloud.java.checks.Preconditions.checkElementIndex;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.StationTrack;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    public static PlayQueue fromTrackUrnList(List<Urn> trackUrns, PlaySessionSource playSessionSource,
                                             Map<Urn, Boolean> blockedTracks) {
        return new PlayQueue(playQueueItemsFromUrns(trackUrns, playSessionSource, blockedTracks));
    }

    public static PlayQueue fromPlayableList(List<PropertySet> playables, PlaySessionSource playSessionSource,
                                             Map<Urn, Boolean> blockedTracks) {
        return new PlayQueue(playQueueItemsFromPlayables(playables, playSessionSource, blockedTracks));
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

    public void removeItemAtPosition(int position) {
        this.playQueueItems.remove(position);
    }

    public boolean hasPreviousItem(int position) {
        return position > 0 && !playQueueItems.isEmpty();
    }

    public boolean hasNextItem(int position) {
        return position < playQueueItems.size() - 1;
    }

    public boolean hasTrackAsNextItem(int position) {
        return hasNextItem(position) && playQueueItems.get(position + 1) instanceof TrackQueueItem;
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
        checkElementIndex(position, size());
        return playQueueItems.get(position).getUrn();
    }

    public Iterable<? extends PlayQueueItem> itemsWithUrn(final Urn urn) {
        return Lists.newArrayList(Iterables.filter(playQueueItems, isMatchingItem(urn)));
    }

    public int indexOfTrackUrn(final Urn trackUrn) {
        return Iterables.indexOf(playQueueItems, isMatchingTrackItem(trackUrn));
    }

    int indexOfPlayQueueItem(final PlayQueueItem playQueueItem) {
        return Iterables.indexOf(playQueueItems, isMatchingItem(playQueueItem));
    }

    public int indexOfTrackUrn(int startPosition, final Urn urn) {
        final List<PlayQueueItem> subList = playQueueItems.subList(startPosition, this.playQueueItems.size());
        final int index = Iterables.indexOf(subList, isMatchingTrackItem(urn));
        if (index >= 0) {
            return index + startPosition;
        } else {
            return index;
        }
    }

    boolean hasSameTracks(PlayQueue playQueue) {
        if (playQueue.size() != size()) {
            return false;
        } else {
            for (int i = 0; i < size(); i++) {
                if (!playQueue.getPlayQueueItem(i).getUrn().equals(getPlayQueueItem(i).getUrn())) {
                    return false;
                }
            }
        }
        return true;
    }

    private Predicate<PlayQueueItem> isMatchingItem(final Urn urn) {
        return new Predicate<PlayQueueItem>() {
            @Override
            public boolean apply(PlayQueueItem input) {
                return input.getUrn().equals(urn);
            }
        };
    }

    private Predicate<PlayQueueItem> isMatchingTrackItem(final Urn urn) {
        return new Predicate<PlayQueueItem>() {
            @Override
            public boolean apply(PlayQueueItem input) {
                return input.isTrack() && input.getUrn().equals(urn);
            }
        };
    }

    public static Predicate<PlayQueueItem> isMatchingItem(final PlayQueueItem playQueueItem) {
        return new Predicate<PlayQueueItem>() {
            @Override
            public boolean apply(PlayQueueItem input) {
                return input.equals(playQueueItem);
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

    public List<Urn> getItemUrns(int from, int count) {
        final int to = Math.min(size(), from + count);
        if (to >= from) {
            final List<Urn> itemUrns = new ArrayList<>(to - from);
            for (int i = from; i < to; i++) {
                itemUrns.add(getUrn(i));
            }
            return itemUrns;
        } else {
            // debugging #5168
            ErrorUtils.handleSilentException(new IllegalStateException("Error getting item urns. size = ["
                    + size() + "], from = [" + from + "], count = [" + count + "]"));
            return Collections.emptyList();
        }
    }

    public boolean shouldPersistItemAt(int position) {
        return position >= 0 && position < playQueueItems.size() && playQueueItems.get(position).shouldPersist();
    }

    public Optional<AdData> getAdData(int position) {
        checkElementIndex(position, size());
        return playQueueItems.get(position).getAdData();
    }

    public static PlayQueue shuffled(List<Urn> offlineAvailable, List<Urn> tracks,
                                     PlaySessionSource playSessionSource, Map<Urn, Boolean> blockedTracks) {
        List<Urn> shuffled = newArrayList(tracks);
        Collections.shuffle(shuffled);

        if (!offlineAvailable.isEmpty()) {
            Collections.shuffle(offlineAvailable);
            Urn randomOfflineTrack = offlineAvailable.get(0);

            shuffled.remove(randomOfflineTrack);
            shuffled.add(0, randomOfflineTrack);
        }

        return fromTrackUrnList(shuffled, playSessionSource, blockedTracks);
    }

    public static PlayQueue shuffled(List<Urn> tracks, PlaySessionSource playSessionSource,
                                     Map<Urn, Boolean> blockedTracks) {
        List<Urn> shuffled = newArrayList(tracks);
        Collections.shuffle(shuffled);
        return fromTrackUrnList(shuffled, playSessionSource, blockedTracks);
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
        checkArgument(position >= 0 && position <= size(),
                String.format(Locale.getDefault(), "Cannot insert item at position:%d, size:%d", position, playQueueItems.size()));
        playQueueItems.add(position, playQueueItem);
    }

    void replaceItem(int position, List<PlayQueueItem> newItems) {
        checkArgument(position >= 0 && position < size(),
                String.format(Locale.getDefault(), "Cannot replace item at position:%d, size:%d", position, newItems.size()));
        playQueueItems.remove(position);
        playQueueItems.addAll(position, newItems);
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

    private static List<PlayQueueItem> playQueueItemsFromUrns(List<Urn> urns,
                                                              final PlaySessionSource playSessionSource,
                                                              final Map<Urn, Boolean> blockedTracks) {
        return newArrayList(transform(urns, new Function<Urn, PlayQueueItem>() {
            @Override
            public PlayQueueItem apply(Urn playable) {
                if (playable.isTrack()) {
                    return new TrackQueueItem.Builder(playable)
                            .fromSource(playSessionSource.getInitialSource(), playSessionSource.getInitialSourceVersion())
                            .blocked(Boolean.TRUE.equals(blockedTracks.get(playable)))
                            .build();

                } else if (playable.isPlaylist()) {
                    return new PlaylistQueueItem.Builder(playable)
                            .fromSource(playSessionSource.getInitialSource(), playSessionSource.getInitialSourceVersion())
                            .build();
                } else {
                    throw new IllegalArgumentException("Unrecognized playable sent for playback " + playable);
                }
            }
        }));
    }

    private static List<PlayQueueItem> playQueueItemsFromPlayables(List<PropertySet> playables,
                                                                   final PlaySessionSource playSessionSource,
                                                                   final Map<Urn, Boolean> blockedTracks) {
        return newArrayList(transform(playables, new Function<PropertySet, PlayQueueItem>() {
            @Override
            public PlayQueueItem apply(PropertySet playable) {

                if (playable.get(EntityProperty.URN).isTrack()) {
                    return new TrackQueueItem.Builder(playable)
                            .fromSource(playSessionSource.getInitialSource(), playSessionSource.getInitialSourceVersion())
                            .blocked(Boolean.TRUE.equals(blockedTracks.get(playable.get(TrackProperty.URN))))
                            .build();

                } else if (playable.get(EntityProperty.URN).isPlaylist()) {
                    return new PlaylistQueueItem.Builder(playable)
                            .fromSource(playSessionSource.getInitialSource(), playSessionSource.getInitialSourceVersion())
                            .build();
                } else {
                    throw new IllegalArgumentException("Unrecognized playable sent for playback " + playable);
                }

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
        return MoreObjects.equal(getTrackItemUrns(), playQueue.getTrackItemUrns());
    }

    @Override
    public int hashCode() {
        return playQueueItems.hashCode();
    }
}
