package com.soundcloud.android.playback;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.ads.AdData;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class PlayQueue implements Iterable<PlayQueueItem> {

    private static final String DEFAULT_SOURCE_VERSION = "default";

    public static PlayQueue empty() {
        return new SimplePlayQueue(new ArrayList(0));
    }

    public static PlayQueue fromTrackUrnList(List<Urn> trackUrns, PlaySessionSource playSessionSource,
                                             Map<Urn, Boolean> blockedTracks) {
        return new SimplePlayQueue(playQueueItemsFromUrns(trackUrns, playSessionSource, blockedTracks));
    }

    public static PlayQueue fromPlayQueueItems(List<PlayQueueItem> playQueueItems) {
        return new SimplePlayQueue(playQueueItems);
    }

    public abstract void moveItem(int fromPosition, int toPosition);

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
                            .withPlaybackContext(PlaybackContext.create(playSessionSource))
                            .blocked(Boolean.TRUE.equals(blockedTracks.get(playable)))
                            .build();

                } else if (playable.isPlaylist()) {
                    return new PlaylistQueueItem.Builder(playable)
                            .fromSource(playSessionSource.getInitialSource(),
                                        playSessionSource.getInitialSourceVersion())
                            .withPlaybackContext(PlaybackContext.create(playSessionSource))
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

    private static List<PlayQueueItem> playQueueItemsFromPlayables(List<PropertySet> playables,
                                                                   final PlaySessionSource playSessionSource,
                                                                   final Map<Urn, Boolean> blockedTracks) {
        return newArrayList(Lists.transform(playables, new Function<PropertySet, PlayQueueItem>() {
            @Override
            public PlayQueueItem apply(PropertySet playable) {

                if (playable.get(EntityProperty.URN).isTrack()) {
                    return new TrackQueueItem.Builder(playable)
                            .fromSource(playSessionSource.getInitialSource(),
                                        playSessionSource.getInitialSourceVersion())
                            .withPlaybackContext(PlaybackContext.create(playSessionSource))
                            .blocked(Boolean.TRUE.equals(blockedTracks.get(playable.get(TrackProperty.URN))))
                            .build();

                } else if (playable.get(EntityProperty.URN).isPlaylist()) {
                    return new PlaylistQueueItem.Builder(playable)
                            .fromSource(playSessionSource.getInitialSource(),
                                        playSessionSource.getInitialSourceVersion())
                            .withPlaybackContext(PlaybackContext.create(playSessionSource))
                            .build();
                } else {
                    throw new IllegalArgumentException("Unrecognized playable sent for playback " + playable);
                }

            }
        }));
    }

    public static PlayQueue fromStation(Urn stationUrn,
                                        List<StationTrack> stationTracks,
                                        PlaySessionSource playSessionSource) {
        List<PlayQueueItem> playQueueItems = new ArrayList<>();
        for (StationTrack stationTrack : stationTracks) {
            final TrackQueueItem.Builder builder = new TrackQueueItem.Builder(stationTrack.getTrackUrn())
                    .relatedEntity(stationUrn)
                    .fromSource(
                            playSessionSource.getInitialSource(),
                            DEFAULT_SOURCE_VERSION,
                            stationUrn,
                            stationTrack.getQueryUrn()
                    )
                    .withPlaybackContext(PlaybackContext.create(playSessionSource));
            playQueueItems.add(builder.build());
        }
        return new SimplePlayQueue(playQueueItems);
    }

    public static PlayQueue fromRecommendations(Urn seedTrack,
                                                boolean continuousPlay,
                                                RecommendedTracksCollection relatedTracks,
                                                PlaySessionSource playSessionSource) {
        return new SimplePlayQueue(playQueueitemsForRecommendations(seedTrack,
                                                                    continuousPlay,
                                                                    relatedTracks,
                                                                    playSessionSource));
    }

    private static List<PlayQueueItem> playQueueitemsForRecommendations(Urn seedTrack,
                                                                        boolean continuousPlay,
                                                                        RecommendedTracksCollection relatedTracks,
                                                                        PlaySessionSource playSessionSource) {
        List<PlayQueueItem> playQueueItems = new ArrayList<>();
        for (ApiTrack relatedTrack : relatedTracks) {
            final PlaybackContext playbackContext = continuousPlay ?
                                                    PlaybackContext.create(PlaybackContext.Bucket.AUTO_PLAY) :
                                                    PlaybackContext.create(playSessionSource);
            final TrackQueueItem.Builder builder = new TrackQueueItem.Builder(relatedTrack.getUrn())
                    .relatedEntity(seedTrack)
                    .fromSource(DiscoverySource.RECOMMENDER.value(), relatedTracks.getSourceVersion())
                    .withPlaybackContext(playbackContext);
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

    public abstract void removeItem(PlayQueueItem item);

    public abstract void removeItemAtPosition(int position);

    public abstract boolean hasPreviousItem(int position);

    public abstract boolean hasNextItem(int position);

    public abstract boolean hasTrackAsNextItem(int position);

    public abstract void insertAllItems(int position, List<PlayQueueItem> playQueueItems);

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

    abstract ShuffledPlayQueue shuffle(int start);

    abstract boolean isShuffled();

    abstract protected List<PlayQueueItem> items();
}
