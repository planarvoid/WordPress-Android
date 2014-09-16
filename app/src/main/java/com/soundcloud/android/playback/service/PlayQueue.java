package com.soundcloud.android.playback.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.soundcloud.android.Consts;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PlayQueue implements Iterable<PlayQueueItem> {

    private final List<PlayQueueItem> playQueueItems;

    public static PlayQueue empty() {
        return new PlayQueue(Collections.<PlayQueueItem>emptyList());
    }

    public static PlayQueue fromTrackUrnList(List<TrackUrn> trackUrns, PlaySessionSource playSessionSource) {
        return new PlayQueue(getPlayQueueItemsFromIds(trackUrns, playSessionSource));
    }

    public PlayQueue copy() {
        return new PlayQueue(new ArrayList<PlayQueueItem>(playQueueItems));
    }

    public PlayQueue(List<PlayQueueItem> playQueueItems) {
        this.playQueueItems = playQueueItems;
    }

    public void addTrack(TrackUrn trackUrn, String source, String sourceVersion) {
        playQueueItems.add(PlayQueueItem.fromTrack(trackUrn, source, sourceVersion));
    }

    public void insertTrack(int position, TrackUrn trackUrn, PropertySet metaData, boolean shouldPersist) {
        checkArgument(position >= 0 && position <= size(), String.format("Cannot insert track at position:%d, size:%d", position, playQueueItems.size()));
        // TODO : Proper source + version?
        playQueueItems.add(position, PlayQueueItem.fromTrack(trackUrn, ScTextUtils.EMPTY_STRING, ScTextUtils.EMPTY_STRING, metaData, shouldPersist));
    }

    public void remove(int position) {
        playQueueItems.remove(position);
    }

    public boolean hasPreviousTrack(int position) {
        return position > 0;
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

    public TrackUrn getUrn(int position){
        return position >= 0 && position < size() ? playQueueItems.get(position).getTrackUrn() : TrackUrn.NOT_SET;
    }

    @Deprecated
    public long getTrackId(int position) {
        return position >= 0 && position < size() ? playQueueItems.get(position).getTrackUrn().numericId : Consts.NOT_SET;
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

        playQueueItems.get(position).getMetaData().merge(metadata);
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
                return input.getTrackUrn().numericId;
            }
        });
        return trackIds;
    }

    List<TrackUrn> getTrackUrns() {
        List<TrackUrn> trackUrns = Lists.transform(playQueueItems, new Function<PlayQueueItem, TrackUrn>() {
            @Override
            public TrackUrn apply(PlayQueueItem input) {
                return input.getTrackUrn();
            }
        });
        return trackUrns;
    }

    private static List<PlayQueueItem> getPlayQueueItemsFromIds(List<TrackUrn> trackIds, final PlaySessionSource playSessionSource) {
        return Lists.newArrayList(Lists.transform(trackIds, new Function<TrackUrn, PlayQueueItem>() {
            @Override
            public PlayQueueItem apply(TrackUrn track) {
                return PlayQueueItem.fromTrack(track, playSessionSource.getInitialSource(), playSessionSource.getInitialSourceVersion(), PropertySet.create(), true);
            }
        }));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayQueue playQueue = (PlayQueue) o;
        return Objects.equal(playQueueItems, playQueue.playQueueItems);
    }

    @Override
    public int hashCode() {
        return playQueueItems.hashCode();
    }
}
