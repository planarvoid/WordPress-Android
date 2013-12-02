package com.soundcloud.android.playback.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.tracking.eventlogger.EventLoggerParamsBuilder;
import com.soundcloud.android.tracking.eventlogger.PlaySessionSource;
import com.soundcloud.android.utils.ScTextUtils;

import android.net.Uri;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

class PlayQueue {

    private List<PlayQueueItem> mPlayQueueItems;
    private int mPosition;
    private PlaySessionSource mPlaySessionSource = PlaySessionSource.EMPTY;
    private boolean mCurrentTrackIsUserTriggered;

    public static PlayQueue empty(){
        return new PlayQueue(Collections.<PlayQueueItem>emptyList(), -1, PlaySessionSource.EMPTY);
    }

    public static PlayQueue fromIdList(List<Long> trackIds, int startPosition, PlaySessionSource playSessionSource) {
        return new PlayQueue(getPlayQueueItemsFromIds(trackIds, playSessionSource), startPosition, playSessionSource);
    }

    public PlayQueue(List<PlayQueueItem> playQueueItems, int startPosition, PlaySessionSource playSessionSource) {
        mPlayQueueItems = playQueueItems;
        mPosition = startPosition;
        mPlaySessionSource = playSessionSource;
    }

    public PlayQueueView getViewWithAppendState(PlaybackOperations.AppendState appendState) {
        return new PlayQueueView(getTrackIds(), mPosition, appendState);
    }

    public Collection<PlayQueueItem> getItems() {
        return mPlayQueueItems;
    }

    public Uri getOriginPage() {
        return mPlaySessionSource.getOriginPage();
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
        if (mPosition < mPlayQueueItems.size() - 1) {
            mPosition++;
            mCurrentTrackIsUserTriggered = userTriggered;
            return true;
        }
        return false;
    }

    public String getCurrentEventLoggerParams() {
        if (isEmpty()) return ScTextUtils.EMPTY_STRING;

        EventLoggerParamsBuilder builder = new EventLoggerParamsBuilder(mCurrentTrackIsUserTriggered);
        builder.origin(getOriginPage());
        builder.source(getCurrentTrackSource());
        builder.sourceVersion(getCurrentTrackSourceVersion());
        if (isPlayingSet()) {
            builder.set(getSetId(), getPosition());
        }
        return builder.build();
    }

    public long getCurrentTrackId() {
        return mPlayQueueItems.get(mPosition).getTrackId();
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

    @VisibleForTesting
    long getSetId() {
        return mPlaySessionSource.getSetId();
    }

    private static List<PlayQueueItem> getPlayQueueItemsFromIds(List<Long> trackIds, final PlaySessionSource playSessionSource){
        return Lists.newArrayList(Lists.transform(trackIds, new Function<Long, PlayQueueItem>() {
            @Override
            public PlayQueueItem apply(Long input) {
                return new PlayQueueItem(input, playSessionSource.getInitialSource(), playSessionSource.getInitialSourceVersion());
            }
        }));
    }

    private boolean isPlayingSet() {
        return getSetId() > Playable.NOT_SET;
    }

    private String getCurrentTrackSource() {
        return mPlayQueueItems.get(mPosition).getSource();
    }
    private String getCurrentTrackSourceVersion() {
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
                Objects.equal(mPlayQueueItems, playQueue.mPlayQueueItems) &&
                Objects.equal(mPlaySessionSource, playQueue.mPlaySessionSource);
    }

    @Override
    public int hashCode() {
        int result = (mCurrentTrackIsUserTriggered ? 1 : 0);
        result = 31 * result + mPlaySessionSource.hashCode();
        result = 31 * result + mPlayQueueItems.hashCode();
        result = 31 * result + mPosition;
        return result;
    }
}
