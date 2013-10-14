package com.soundcloud.android.service.playback;

import com.google.common.base.Objects;

import java.util.Collections;
import java.util.List;

public class PlayQueueState {
    public static final PlayQueueState EMPTY = new PlayQueueState(Collections.<Long>emptyList(), 0, AppendState.IDLE);

    private final int mPlayPosition;
    private List<Long> mTrackIds;
    private AppendState mAppendState;

    public enum AppendState {
        IDLE, LOADING, ERROR, EMPTY;
    }
    public PlayQueueState(List<Long> currentTrackIds, int playPosition, AppendState currentAppendState) {
        mTrackIds = currentTrackIds;
        mPlayPosition = playPosition;
        mAppendState = currentAppendState;
    }

    public List<Long> getCurrentTrackIds() {
        return mTrackIds;
    }

    public int getPlayPosition() {
        return mPlayPosition;
    }

    public AppendState getCurrentAppendState() {
        return mAppendState;
    }

    public boolean isLoading() {
        return mAppendState == PlayQueueState.AppendState.LOADING;
    }

    public boolean lastLoadFailed() {
        return mAppendState == PlayQueueState.AppendState.ERROR;
    }

    public boolean lastLoadWasEmpty() {
        return mAppendState == PlayQueueState.AppendState.EMPTY;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("Track IDs", mTrackIds)
                .add("Play Position", mPlayPosition)
                .add("Append State", mAppendState)
                .toString();
    }
}
