package com.soundcloud.android.playback.service;

import static com.soundcloud.android.playback.service.PlayQueueManager.FetchRecommendedState;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.soundcloud.android.Consts;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.tracks.TrackUrn;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PlayQueue implements Iterable<PlayQueueItem> {

    private final List<PlayQueueItem> playQueueItems;
    private int currentPosition;
    private boolean currentTrackIsUserTriggered;

    public static PlayQueue empty() {
        return new PlayQueue(Collections.<PlayQueueItem>emptyList(), Consts.NOT_SET);
    }

    public static PlayQueue fromIdList(List<Long> trackIds, int startPosition, PlaySessionSource playSessionSource) {
        return new PlayQueue(getPlayQueueItemsFromIds(trackIds, playSessionSource), startPosition);
    }

    public PlayQueue(List<PlayQueueItem> playQueueItems, int startPosition) {
        this.playQueueItems = playQueueItems;
        currentPosition = startPosition;
    }

    @Deprecated
    public PlayQueueView getViewWithAppendState(FetchRecommendedState fetchState) {
        return new PlayQueueView(getTrackIds(), currentPosition, fetchState);
    }

    public void setCurrentTrackToUserTriggered() {
        currentTrackIsUserTriggered = true;
    }

    public void addTrack(long id, String source, String sourceVersion) {
        playQueueItems.add(PlayQueueItem.fromTrack(id, source, sourceVersion));
    }

    public void insertAudioAdAtPosition(AudioAd audioAd, int position) {
        playQueueItems.add(position, PlayQueueItem.fromAudioAd(audioAd));
    }

    public void removeAtPosition(int position) {
        playQueueItems.remove(position);
    }

    public boolean moveToPrevious() {
        if (hasPreviousTrack()) {
            currentPosition--;
            currentTrackIsUserTriggered = true;
            return true;
        }
        return false;
    }

    public boolean moveToNext(boolean userTriggered) {
        if (hasNextTrack()) {
            currentPosition++;
            currentTrackIsUserTriggered = userTriggered;
            return true;
        }
        return false;
    }

    public boolean hasPreviousTrack() {
        return currentPosition > 0;
    }

    public boolean hasNextTrack() {
        return currentPosition < playQueueItems.size() - 1;
    }

    @Override
    public Iterator<PlayQueueItem> iterator() {
        return playQueueItems.iterator();
    }

    public int size() {
        return playQueueItems.size();
    }

    @Nullable
    TrackSourceInfo getCurrentTrackSourceInfo(PlaySessionSource playSessionSource) {
        if (isEmpty()) {
            return null;
        }

        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(playSessionSource.getOriginScreen(), currentTrackIsUserTriggered);
        trackSourceInfo.setSource(getCurrentTrackSource(), getCurrentTrackSourceVersion());
        if (playSessionSource.getPlaylistId() != PublicApiPlaylist.NOT_SET) {
            trackSourceInfo.setOriginPlaylist(playSessionSource.getPlaylistId(), getCurrentPosition(), playSessionSource.getPlaylistOwnerId());
        }
        return trackSourceInfo;
    }

    @Deprecated
    public long getCurrentTrackId() {
        return currentPosition < 0 || currentPosition >= playQueueItems.size() ? Consts.NOT_SET : playQueueItems.get(currentPosition).getTrackId();
    }

    public TrackUrn getCurrentTrackUrn() {
        return getUrnAtPosition(currentPosition);
    }

    public boolean isEmpty() {
        return playQueueItems.isEmpty();
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public boolean setPosition(int position) {
        if (position < playQueueItems.size()) {
            this.currentPosition = position;
            return true;
        } else {
            return false;
        }
    }

    public TrackUrn getUrnAtPosition(int position){
        return position >= 0 && position < size() ? playQueueItems.get(position).getTrackUrn() : TrackUrn.NOT_SET;
    }

    private static List<PlayQueueItem> getPlayQueueItemsFromIds(List<Long> trackIds, final PlaySessionSource playSessionSource) {
        return Lists.newArrayList(Lists.transform(trackIds, new Function<Long, PlayQueueItem>() {
            @Override
            public PlayQueueItem apply(Long input) {
                return PlayQueueItem.fromTrack(input, playSessionSource.getInitialSource(), playSessionSource.getInitialSourceVersion());
            }
        }));
    }

    String getCurrentTrackSource() {
        return playQueueItems.get(currentPosition).getSource();
    }

    String getCurrentTrackSourceVersion() {
        return playQueueItems.get(currentPosition).getSourceVersion();
    }

    boolean isAudioAd(int position) {
        return position >= 0 && position < size() && playQueueItems.get(position).isAudioAd();
    }

    private List<Long> getTrackIds() {
        List<Long> trackIds = Lists.transform(playQueueItems, new Function<PlayQueueItem, Long>() {
            @Override
            public Long apply(PlayQueueItem input) {
                return input.getTrackId();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayQueue playQueue = (PlayQueue) o;

        return Objects.equal(currentTrackIsUserTriggered, playQueue.currentTrackIsUserTriggered) &&
                Objects.equal(currentPosition, playQueue.currentPosition) &&
                Objects.equal(playQueueItems, playQueue.playQueueItems);
    }

    @Override
    public int hashCode() {
        int result = (currentTrackIsUserTriggered ? 1 : 0);
        result = 31 * result + playQueueItems.hashCode();
        result = 31 * result + currentPosition;
        return result;
    }
}
