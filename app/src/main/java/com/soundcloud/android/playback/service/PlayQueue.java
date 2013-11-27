package com.soundcloud.android.playback.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.tracking.eventlogger.PlaySessionSource;
import com.soundcloud.android.tracking.eventlogger.TrackSourceInfo;
import com.soundcloud.android.utils.ScTextUtils;

import android.net.Uri;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class PlayQueue {

    // TODO, get rid of this, it is mutable
    public static final PlayQueue EMPTY = new PlayQueue(Collections.<Long>emptyList(), -1,
            PlaySessionSource.EMPTY, TrackSourceInfo.EMPTY);

    private boolean mCurrentTrackIsUserTriggered;
    private PlaySessionSource mPlaySessionSource;

    private List<PlayQueueItem> mPlayQueueItems;
    private int mPosition;

    public PlayQueue(List<Long> currentTrackIds, int startPosition,
                     PlaySessionSource playSessionSource, TrackSourceInfo initialTrackSourceInfo) {
        setPlayQueueFromIds(currentTrackIds, initialTrackSourceInfo);
        mPosition = startPosition;
        mPlaySessionSource = playSessionSource;
    }

    public PlayQueue(List<Long> trackIds, int playPosition, PlaySessionSource playSessionSource) {
        this(trackIds, playPosition, playSessionSource, TrackSourceInfo.EMPTY);
    }

    @VisibleForTesting
    PlayQueue(ArrayList<Long> trackIds, int position) {
        this(trackIds, position, PlaySessionSource.EMPTY, TrackSourceInfo.EMPTY);

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

    private void setPlayQueueFromIds(List<Long> trackIds, final TrackSourceInfo trackSourceInfo){
        mPlayQueueItems = Lists.newArrayList(Lists.transform(trackIds, new Function<Long, PlayQueueItem>() {
            @Nullable
            @Override
            public PlayQueueItem apply(@Nullable Long input) {
                return new PlayQueueItem(input, trackSourceInfo.getSource(), trackSourceInfo.getSourceVersion());
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

    public PlaySessionSource getPlaySessionSource() {
        return mPlaySessionSource;
    }

    /* package */ Uri getPlayQueueState(long seekPos, long currentTrackId) {
        return new PlayQueueUri().toUri(currentTrackId, mPosition, seekPos, mPlaySessionSource);
    }

    public void addTrack(long id, TrackSourceInfo trackSourceInfo) {
        mPlayQueueItems.add(new PlayQueueItem(id, trackSourceInfo.getSource(), trackSourceInfo.getSourceVersion()));
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
        return ScTextUtils.EMPTY_STRING;
//        if (isEmpty()) return ScTextUtils.EMPTY_STRING;
//
//        final TrackSourceInfo trackSourceInfo = mTrackSourceInfoMap.get(getCurrentTrackId());
//        trackSourceInfo.setTrigger(mCurrentTrackIsUserTriggered);
//
//        final String originUrl = "PUT CONTEXT HERE";
//        if (mOriginPage != null && Content.match(mOriginPage) == Content.PLAYLIST) {
//            return trackSourceInfo.createEventLoggerParamsForSet(mOriginPage.getLastPathSegment(), String.valueOf(mPosition), originUrl);
//        } else {
//            return trackSourceInfo.createEventLoggerParams(originUrl);
//        }
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

    private List<Long> getTrackIds(){
        List<Long> trackIds = Lists.transform(mPlayQueueItems, new Function<PlayQueueItem, Long>() {
            @Override
            public Long apply(PlayQueueItem input) {
                return input.getId();
            }
        });
        return trackIds;
    }
}
