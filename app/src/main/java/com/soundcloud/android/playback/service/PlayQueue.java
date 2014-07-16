package com.soundcloud.android.playback.service;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.soundcloud.android.Consts;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.tracks.TrackUrn;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PlayQueue implements Iterable<PlayQueueItem> {

    private final List<PlayQueueItem> playQueueItems;

    public static PlayQueue empty() {
        return new PlayQueue(Collections.<PlayQueueItem>emptyList());
    }

    public static PlayQueue fromIdList(List<Long> trackIds, PlaySessionSource playSessionSource) {
        return new PlayQueue(getPlayQueueItemsFromIds(trackIds, playSessionSource));
    }

    public PlayQueue(List<PlayQueueItem> playQueueItems) {
        this.playQueueItems = playQueueItems;
    }

    public void addTrack(TrackUrn trackUrn, String source, String sourceVersion) {
        playQueueItems.add(PlayQueueItem.fromTrack(trackUrn, source, sourceVersion));
    }

    public void insertAudioAd(AudioAd audioAd, int position) {
        playQueueItems.add(position, PlayQueueItem.fromAudioAd(audioAd));
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

    public TrackUrn getUrn(int position){
        return position >= 0 && position < size() ? playQueueItems.get(position).getTrackUrn() : TrackUrn.NOT_SET;
    }

    @Deprecated
    public long getTrackId(int position) {
        return position >= 0 && position < size() ? playQueueItems.get(position).getTrackUrn().numericId : Consts.NOT_SET;
    }

    String getTrackSource(int position) {
        return playQueueItems.get(position).getSource();
    }

    String getSourceVersion(int position) {
        return playQueueItems.get(position).getSourceVersion();
    }

    boolean isAudioAd(int position) {
        return position >= 0 && position < size() && playQueueItems.get(position).isAudioAd();
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

    private static List<PlayQueueItem> getPlayQueueItemsFromIds(List<Long> trackIds, final PlaySessionSource playSessionSource) {
        return Lists.newArrayList(Lists.transform(trackIds, new Function<Long, PlayQueueItem>() {
            @Override
            public PlayQueueItem apply(Long trackId) {
                return PlayQueueItem.fromTrack(TrackUrn.forTrack(trackId), playSessionSource);
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
