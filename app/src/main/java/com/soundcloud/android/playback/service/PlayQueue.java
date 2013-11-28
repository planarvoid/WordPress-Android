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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class PlayQueue {

    // TODO, get rid of this, it is mutable
    public static final PlayQueue EMPTY = new PlayQueue(Collections.<Long>emptyList(), -1, PlaySessionSource.EMPTY);

    private boolean mCurrentTrackIsUserTriggered;
    private PlaySessionSource mPlaySessionSource = PlaySessionSource.EMPTY;

    private List<PlayQueueItem> mPlayQueueItems;
    private int mPosition;

    public PlayQueue(List<Long> trackIds, int startPosition, PlaySessionSource playSessionSource) {
        setPlayQueueFromIds(trackIds, playSessionSource);
        mPosition = startPosition;
        mPlaySessionSource = playSessionSource;
    }

    @VisibleForTesting
    PlayQueue(ArrayList<Long> trackIds, int position) {
        this(trackIds, position, PlaySessionSource.EMPTY);
    }

    @VisibleForTesting
    PlayQueue(long trackId) {
     this(Lists.newArrayList(trackId), 0);
    }

    public PlayQueue(List<PlayQueueItem> playQueueItems, PlaySessionSource playSessionSource) {
        mPlayQueueItems = playQueueItems;
        mPlaySessionSource = playSessionSource;
    }

    public PlayQueueView getViewWithAppendState(PlaybackOperations.AppendState appendState) {
        return new PlayQueueView(getTrackIds(), mPosition, appendState);
    }

    public Collection<PlayQueueItem> getItems() {
        return mPlayQueueItems;
    }

    private void setPlayQueueFromIds(List<Long> trackIds, final PlaySessionSource playSessionSource){
        mPlayQueueItems = Lists.newArrayList(Lists.transform(trackIds, new Function<Long, PlayQueueItem>() {
            @Nullable
            @Override
            public PlayQueueItem apply(@Nullable Long input) {
                return new PlayQueueItem(input, playSessionSource.getInitialSource(), playSessionSource.getInitialSourceVersion());
            }
        }));
    }

    public Uri getOriginPage() {
        return mPlaySessionSource.getOriginPage();
    }

    public long getSetId() {
        return mPlaySessionSource.getSetId();
    }

    public void setCurrentTrackToUserTriggered() {
        mCurrentTrackIsUserTriggered = true;
    }

    /* package */ Uri getPlayQueueState(long seekPos, long currentTrackId) {
        return new PlayQueueUri().toUri(currentTrackId, mPosition, seekPos, mPlaySessionSource);
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

    private boolean isPlayingSet() {
        return getSetId() > Playable.NOT_SET;
    }

    public long getCurrentTrackId() {
        return mPlayQueueItems.get(mPosition).getTrackId();
    }

    public String getCurrentTrackSource() {
        return mPlayQueueItems.get(mPosition).getSource();
    }
    public String getCurrentTrackSourceVersion() {
        return mPlayQueueItems.get(mPosition).getSourceVersion();
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
