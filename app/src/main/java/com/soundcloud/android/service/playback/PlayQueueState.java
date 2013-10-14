package com.soundcloud.android.service.playback;

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

    public boolean isFetchingRelated() {
        return mAppendState == PlayQueueState.AppendState.LOADING;
    }

    public boolean lastRelatedFetchFailed() {
        return mAppendState == PlayQueueState.AppendState.ERROR;
    }

    public boolean lastRelatedFetchWasEmpty() {
        return mAppendState == PlayQueueState.AppendState.EMPTY;
    }
}
