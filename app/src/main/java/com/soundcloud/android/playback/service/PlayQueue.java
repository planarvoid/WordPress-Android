package com.soundcloud.android.playback.service;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackOperations;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

class PlayQueue {

    private List<PlayQueueItem> mPlayQueueItems;
    private int mPosition;
    private boolean mCurrentTrackIsUserTriggered;

    public static PlayQueue empty(){
        return new PlayQueue(Collections.<PlayQueueItem>emptyList(), -1);
    }

    public static PlayQueue fromIdList(List<Long> trackIds, int startPosition, PlaySessionSource playSessionSource) {
        return new PlayQueue(getPlayQueueItemsFromIds(trackIds, playSessionSource), startPosition);
    }

    public PlayQueue(List<PlayQueueItem> playQueueItems, int startPosition) {
        mPlayQueueItems = playQueueItems;
        mPosition = startPosition;
    }

    public PlayQueueView getViewWithAppendState(PlaybackOperations.AppendState appendState) {
        return new PlayQueueView(getTrackIds(), mPosition, appendState);
    }

    public Collection<PlayQueueItem> getItems() {
        return mPlayQueueItems;
    }

    public void setCurrentTrackToUserTriggered() {
        mCurrentTrackIsUserTriggered = true;
    }

    public void addTrack(long id, String source, String sourceVersion) {
        mPlayQueueItems.add(new PlayQueueItem(id, source, sourceVersion));
    }

    public boolean moveToPrevious() {
        if (mPosition > 0) {
            mPosition--;
            mCurrentTrackIsUserTriggered = true;
            return true;
        }
        return false;
    }

    public boolean moveToNext(boolean userTriggered) {
        if (hasNextTrack()) {
            mPosition++;
            mCurrentTrackIsUserTriggered = userTriggered;
            return true;
        }
        return false;
    }

    public boolean hasNextTrack(){
        return mPosition < mPlayQueueItems.size() - 1;
    }

    @Nullable
    TrackSourceInfo getCurrentTrackSourceInfo(PlaySessionSource playSessionSource) {
        if (isEmpty()) return null;

        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(playSessionSource.getOriginScreen(), mCurrentTrackIsUserTriggered);
        trackSourceInfo.setSource(getCurrentTrackSource(), getCurrentTrackSourceVersion());
        if (playSessionSource.getPlaylistId() != Playlist.NOT_SET) {
            trackSourceInfo.setOriginPlaylist(playSessionSource.getPlaylistId(), getPosition(), playSessionSource.getPlaylistOwnerId());
        }
        return trackSourceInfo;
    }

    public long getCurrentTrackId() {
        return mPosition < 0 || mPosition >= mPlayQueueItems.size() ? Track.NOT_SET : mPlayQueueItems.get(mPosition).getTrackId();
    }

    public boolean isEmpty() {
        return mPlayQueueItems.isEmpty();
    }

    public int getPosition() {
        return mPosition;
    }

    public boolean setPosition(int position) {
        if (position < mPlayQueueItems.size()) {
            mPosition = position;
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
        return mPlayQueueItems.get(mPosition).getSource();
    }
    String getCurrentTrackSourceVersion() {
        return mPlayQueueItems.get(mPosition).getSourceVersion();
    }

    private List<Long> getTrackIds(){
        List<Long> trackIds = Lists.transform(mPlayQueueItems, new Function<PlayQueueItem, Long>() {
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

        return Objects.equal(mCurrentTrackIsUserTriggered, playQueue.mCurrentTrackIsUserTriggered) &&
                Objects.equal(mPosition, playQueue.mPosition) &&
                Objects.equal(mPlayQueueItems, playQueue.mPlayQueueItems);
    }

    @Override
    public int hashCode() {
        int result = (mCurrentTrackIsUserTriggered ? 1 : 0);
        result = 31 * result + mPlayQueueItems.hashCode();
        result = 31 * result + mPosition;
        return result;
    }
}
