package com.soundcloud.android.playback.service;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackOperations;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

class PlayQueue {

    private List<PlayQueueItem> playQueueItems;
    private int position;
    private boolean currentTrackIsUserTriggered;

    public static PlayQueue empty(){
        return new PlayQueue(Collections.<PlayQueueItem>emptyList(), -1);
    }

    public static PlayQueue fromIdList(List<Long> trackIds, int startPosition, PlaySessionSource playSessionSource) {
        return new PlayQueue(getPlayQueueItemsFromIds(trackIds, playSessionSource), startPosition);
    }

    public PlayQueue(List<PlayQueueItem> playQueueItems, int startPosition) {
        this.playQueueItems = playQueueItems;
        position = startPosition;
    }

    public PlayQueueView getViewWithAppendState(PlaybackOperations.AppendState appendState) {
        return new PlayQueueView(getTrackIds(), position, appendState);
    }

    public List<PlayQueueItem> getItems() {
        return playQueueItems;
    }

    public void setCurrentTrackToUserTriggered() {
        currentTrackIsUserTriggered = true;
    }

    public void addTrack(long id, String source, String sourceVersion) {
        playQueueItems.add(new PlayQueueItem(id, source, sourceVersion));
    }

    public boolean moveToPrevious() {
        if (hasPreviousTrack()) {
            position--;
            currentTrackIsUserTriggered = true;
            return true;
        }
        return false;
    }

    public boolean moveToNext(boolean userTriggered) {
        if (hasNextTrack()) {
            position++;
            currentTrackIsUserTriggered = userTriggered;
            return true;
        }
        return false;
    }

    public boolean hasPreviousTrack() {
        return position > 0;
    }

    public boolean hasNextTrack(){
        return position < playQueueItems.size() - 1;
    }

    @Nullable
    TrackSourceInfo getCurrentTrackSourceInfo(PlaySessionSource playSessionSource) {
        if (isEmpty()) return null;

        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(playSessionSource.getOriginScreen(), currentTrackIsUserTriggered);
        trackSourceInfo.setSource(getCurrentTrackSource(), getCurrentTrackSourceVersion());
        if (playSessionSource.getPlaylistId() != Playlist.NOT_SET) {
            trackSourceInfo.setOriginPlaylist(playSessionSource.getPlaylistId(), getPosition(), playSessionSource.getPlaylistOwnerId());
        }
        return trackSourceInfo;
    }

    public long getCurrentTrackId() {
        return position < 0 || position >= playQueueItems.size() ? Track.NOT_SET : playQueueItems.get(position).getTrackId();
    }

    public boolean isEmpty() {
        return playQueueItems.isEmpty();
    }

    public int getPosition() {
        return position;
    }

    public boolean setPosition(int position) {
        if (position < playQueueItems.size()) {
            this.position = position;
            return true;
        } else {
            return false;
        }
    }

    private static List<PlayQueueItem> getPlayQueueItemsFromIds(List<Long> trackIds, final PlaySessionSource playSessionSource){
        return Lists.newArrayList(Lists.transform(trackIds, new Function<Long, PlayQueueItem>() {
            @Override
            public PlayQueueItem apply(Long input) {
                return new PlayQueueItem(input, playSessionSource.getInitialSource(), playSessionSource.getInitialSourceVersion());
            }
        }));
    }

    String getCurrentTrackSource() {
        return playQueueItems.get(position).getSource();
    }
    String getCurrentTrackSourceVersion() {
        return playQueueItems.get(position).getSourceVersion();
    }

    private List<Long> getTrackIds(){
        List<Long> trackIds = Lists.transform(playQueueItems, new Function<PlayQueueItem, Long>() {
            @Override
            public Long apply(PlayQueueItem input) {
                return input.getTrackId();
            }
        });
        return trackIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayQueue playQueue = (PlayQueue) o;

        return Objects.equal(currentTrackIsUserTriggered, playQueue.currentTrackIsUserTriggered) &&
                Objects.equal(position, playQueue.position) &&
                Objects.equal(playQueueItems, playQueue.playQueueItems);
    }

    @Override
    public int hashCode() {
        int result = (currentTrackIsUserTriggered ? 1 : 0);
        result = 31 * result + playQueueItems.hashCode();
        result = 31 * result + position;
        return result;
    }
}
