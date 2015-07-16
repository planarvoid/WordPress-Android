package com.soundcloud.android.playback;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PlayQueue implements Iterable<PlayQueueItem> {

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
        return new PlayQueue(getPlayQueueItemsFromIds(trackUrns, playSessionSource));
    }

    public PlayQueue copy() {
        return new PlayQueue(new ArrayList<>(playQueueItems));
    }

    public PlayQueue(List<PlayQueueItem> playQueueItems) {
        this.playQueueItems = playQueueItems;
    }

    public void addTrack(Urn trackUrn, String source, String sourceVersion) {
        playQueueItems.add(PlayQueueItem.fromTrack(trackUrn, source, sourceVersion));
    }

    public void insertTrack(int position, Urn trackUrn, PropertySet metaData, boolean shouldPersist) {
        checkArgument(position >= 0 && position <= size(), String.format("Cannot insert track at position:%d, size:%d", position, playQueueItems.size()));
        // TODO : Proper source + version?
        playQueueItems.add(position, PlayQueueItem.fromTrack(trackUrn, ScTextUtils.EMPTY_STRING, ScTextUtils.EMPTY_STRING, metaData, shouldPersist));
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
        return Lists.transform(playQueueItems, toUrn).indexOf(initialTrack);
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

    public void mergeMetaData(int position, PropertySet metadata) {
        checkElementIndex(position, size());

        playQueueItems.get(position).getMetaData().update(metadata);
    }

    public static PlayQueue shuffled(List<Urn> tracks, PlaySessionSource playSessionSource) {
        List<Urn> shuffled = Lists.newArrayList(tracks);
        Collections.shuffle(shuffled);
        return fromTrackUrnList(shuffled, playSessionSource);
    }

    public static PlayQueue fromRecommendations(RecommendedTracksCollection relatedTracks) {
        List<PlayQueueItem> playQueueItems = new ArrayList<>();

        for (ApiTrack relatedTrack : relatedTracks) {
            playQueueItems.add(PlayQueueItem.fromTrack(relatedTrack.getUrn(), PlaySessionSource.DiscoverySource.RECOMMENDER.value(),
                    relatedTracks.getSourceVersion()));
        }
        return new PlayQueue(playQueueItems);
    }

    public static PlayQueue fromRecommendations(Urn seedTrack, RecommendedTracksCollection relatedTracks) {
        PlayQueue playQueue = fromRecommendations(relatedTracks);
        playQueue.playQueueItems.add(0, PlayQueueItem.fromTrack(seedTrack));
        return playQueue;
    }

    public void addPlayQueueItem(PlayQueueItem playQueueItem) {
        playQueueItems.add(playQueueItem);
    }


    String getTrackSource(int position) {
        return playQueueItems.get(position).getSource();
    }

    String getSourceVersion(int position) {
        return playQueueItems.get(position).getSourceVersion();
    }

    List<Long> getTrackIds() {
        List<Long> trackIds = Lists.transform(playQueueItems, new Function<PlayQueueItem, Long>() {
            @Override
            public Long apply(PlayQueueItem input) {
                return input.getTrackUrn().getNumericId();
            }
        });
        return trackIds;
    }

    List<Urn> getTrackUrns() {
        List<Urn> trackUrns = Lists.transform(playQueueItems, new Function<PlayQueueItem, Urn>() {
            @Override
            public Urn apply(PlayQueueItem input) {
                return input.getTrackUrn();
            }
        });
        return trackUrns;
    }

    private static List<PlayQueueItem> getPlayQueueItemsFromIds(List<Urn> trackIds, final PlaySessionSource playSessionSource) {
        return Lists.newArrayList(Lists.transform(trackIds, new Function<Urn, PlayQueueItem>() {
            @Override
            public PlayQueueItem apply(Urn track) {
                return PlayQueueItem.fromTrack(track, playSessionSource.getInitialSource(), playSessionSource.getInitialSourceVersion(), PropertySet.create(), true);
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
        return Objects.equal(playQueueItems, playQueue.playQueueItems);
    }

    @Override
    public int hashCode() {
        return playQueueItems.hashCode();
    }
}
