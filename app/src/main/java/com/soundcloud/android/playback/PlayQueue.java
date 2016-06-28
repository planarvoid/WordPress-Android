package com.soundcloud.android.playback;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.StationTrack;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class PlayQueue implements Iterable<PlayQueueItem> {

    private static final String DEFAULT_SOURCE_VERSION = "default";

    public static PlayQueue empty() {
        return new SimplePlayQueue(Collections.<PlayQueueItem>emptyList());
    }

    public static PlayQueue fromTrackUrnList(List<Urn> trackUrns, PlaySessionSource playSessionSource,
                                             Map<Urn, Boolean> blockedTracks) {
        return new SimplePlayQueue(playQueueItemsFromUrns(trackUrns, playSessionSource, blockedTracks));
    }

    public static PlayQueue fromPlayQueueItems(List<PlayQueueItem> playQueueItems) {
        return new SimplePlayQueue(playQueueItems);
    }

    private static List<PlayQueueItem> playQueueItemsFromUrns(List<Urn> urns,
                                                              final PlaySessionSource playSessionSource,
                                                              final Map<Urn, Boolean> blockedTracks) {
        return newArrayList(Lists.transform(urns, new Function<Urn, PlayQueueItem>() {
            @Override
            public PlayQueueItem apply(Urn playable) {
                if (playable.isTrack()) {
                    return new TrackQueueItem.Builder(playable)
                            .fromSource(playSessionSource.getInitialSource(),
                                        playSessionSource.getInitialSourceVersion())
                            .blocked(Boolean.TRUE.equals(blockedTracks.get(playable)))
                            .build();

                } else if (playable.isPlaylist()) {
                    return new PlaylistQueueItem.Builder(playable)
                            .fromSource(playSessionSource.getInitialSource(),
                                        playSessionSource.getInitialSourceVersion())
                            .build();
                } else {
                    throw new IllegalArgumentException("Unrecognized playable sent for playback " + playable);
                }
            }
        }));
    }


    public static PlayQueue fromPlayableList(List<PropertySet> playables, PlaySessionSource playSessionSource,
                                             Map<Urn, Boolean> blockedTracks) {
        return new SimplePlayQueue(playQueueItemsFromPlayables(playables,
                                                               playSessionSource,
                                                               blockedTracks));
    }

    public static List<PlayQueueItem> playQueueItemsFromPlayables(List<PropertySet> playables,
                                                                  final PlaySessionSource playSessionSource,
                                                                  final Map<Urn, Boolean> blockedTracks) {
        return newArrayList(Lists.transform(playables, new Function<PropertySet, PlayQueueItem>() {
            @Override
            public PlayQueueItem apply(PropertySet playable) {

                if (playable.get(EntityProperty.URN).isTrack()) {
                    return new TrackQueueItem.Builder(playable)
                            .fromSource(playSessionSource.getInitialSource(),
                                        playSessionSource.getInitialSourceVersion())
                            .blocked(Boolean.TRUE.equals(blockedTracks.get(playable.get(TrackProperty.URN))))
                            .build();

                } else if (playable.get(EntityProperty.URN).isPlaylist()) {
                    return new PlaylistQueueItem.Builder(playable)
                            .fromSource(playSessionSource.getInitialSource(),
                                        playSessionSource.getInitialSourceVersion())
                            .build();
                } else {
                    throw new IllegalArgumentException("Unrecognized playable sent for playback " + playable);
                }

            }
        }));
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
        return fromStation(stationUrn, stationTracks, DiscoverySource.STATIONS);
    }

    public static PlayQueue fromStation(Urn stationUrn,
                                        List<StationTrack> stationTracks,
                                        DiscoverySource discoverySource) {
        List<PlayQueueItem> playQueueItems = new ArrayList<>();
        for (StationTrack stationTrack : stationTracks) {
            final TrackQueueItem.Builder builder = new TrackQueueItem.Builder(stationTrack.getTrackUrn())
                    .relatedEntity(stationUrn)
                    .fromSource(
                            discoverySource.value(),
                            DEFAULT_SOURCE_VERSION,
                            stationUrn,
                            stationTrack.getQueryUrn()
                    );
            playQueueItems.add(builder.build());
        }
        return new SimplePlayQueue(playQueueItems);
    }

    public static PlayQueue fromRecommendations(Urn seedTrack, RecommendedTracksCollection relatedTracks) {
        return new SimplePlayQueue(playQueueitemsForRecommendations(seedTrack, relatedTracks));
    }

    public static PlayQueue fromRecommendationsWithPrependedSeed(Urn seedTrack,
                                                                 RecommendedTracksCollection relatedTracks) {

        List<PlayQueueItem> playQueueItems = playQueueitemsForRecommendations(seedTrack, relatedTracks);
        playQueueItems.add(0, new TrackQueueItem.Builder(seedTrack).build());
        return new SimplePlayQueue(playQueueItems);
    }

    private static List<PlayQueueItem> playQueueitemsForRecommendations(Urn seedTrack,
                                                                        RecommendedTracksCollection relatedTracks) {
        List<PlayQueueItem> playQueueItems = new ArrayList<>();
        for (ApiTrack relatedTrack : relatedTracks) {
            final TrackQueueItem.Builder builder = new TrackQueueItem.Builder(relatedTrack.getUrn())
                    .relatedEntity(seedTrack)
                    .fromSource(DiscoverySource.RECOMMENDER.value(),
                                relatedTracks.getSourceVersion());
            playQueueItems.add(builder.build());
        }
        return playQueueItems;
    }

    public static Predicate<PlayQueueItem> isMatchingItem(final PlayQueueItem playQueueItem) {
        return new Predicate<PlayQueueItem>() {
            @Override
            public boolean apply(PlayQueueItem input) {
                return input.equals(playQueueItem);
            }
        };
    }

    public abstract PlayQueue copy();

    public abstract PlayQueueItem getPlayQueueItem(int position);

    public abstract void insertAudioAd(int position, Urn trackUrn, AudioAd adData, boolean shouldPersist);

    public abstract void insertVideo(int position, VideoAd videoAd);

    public abstract void removeItemAtPosition(int position);

    public abstract boolean hasPreviousItem(int position);

    public abstract boolean hasNextItem(int position);

    public abstract boolean hasTrackAsNextItem(int position);

    @Override
    public abstract Iterator<PlayQueueItem> iterator();

    public abstract int size();

    public abstract boolean isEmpty();

    public abstract boolean hasItems();

    public abstract Urn getUrn(int position);

    public abstract Iterable<? extends PlayQueueItem> itemsWithUrn(final Urn urn);

    public abstract int indexOfTrackUrn(final Urn trackUrn);

    abstract int indexOfPlayQueueItem(final PlayQueueItem playQueueItem);

    public abstract int indexOfTrackUrn(int startPosition, final Urn urn);

    abstract boolean hasSameTracks(PlayQueue playQueue);

    public abstract List<Urn> getTrackItemUrns();

    public abstract List<Urn> getItemUrns(int from, int count);

    public abstract boolean shouldPersistItemAt(int position);

    public abstract Optional<AdData> getAdData(int position);

    abstract void insertPlayQueueItem(int position, PlayQueueItem playQueueItem);

    abstract void replaceItem(int position, List<PlayQueueItem> newItems);

    abstract void addPlayQueueItem(PlayQueueItem playQueueItem);

    abstract void addAllPlayQueueItems(Iterable<PlayQueueItem> somePlayQueueItems);

    abstract List<Integer> getQueueHashes();


}
