package com.soundcloud.android.service.playback;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.List;

public class PlayQueueState implements Parcelable {
    public static String EXTRA = "PlayQueueState";
    public static final PlayQueueState EMPTY = new PlayQueueState(Collections.<Long>emptyList(), 0, AppendState.IDLE);

    private final int mPlayPosition;
    private List<Long> mTrackIds = Collections.emptyList();
    private AppendState mAppendState = AppendState.EMPTY;
    private PlaySourceInfo mPlaySourceInfo;

    public enum AppendState {
        IDLE, LOADING, ERROR, EMPTY;
    }

    public PlayQueueState(List<Long> currentTrackIds, int playPosition, PlaySourceInfo playSourceInfo) {
        mTrackIds = currentTrackIds;
        mPlayPosition = playPosition;
        mPlaySourceInfo = playSourceInfo;
    }

    public PlayQueueState(List<Long> currentTrackIds, int playPosition, AppendState currentAppendState) {
        this(currentTrackIds, playPosition, PlaySourceInfo.EMPTY);
        mAppendState = currentAppendState;
    }

    public PlayQueueState(Parcel in) {
        long[] trackIds = new long[in.readInt()];
        in.readLongArray(trackIds);

        mTrackIds = Lists.newArrayListWithExpectedSize(trackIds.length);
        for (long n : trackIds) mTrackIds.add(n);

        mPlayPosition = in.readInt();
        mAppendState = AppendState.valueOf(in.readString());
        mPlaySourceInfo = new PlaySourceInfo(in.readBundle());
    }

    public boolean isEmpty(){
        return mTrackIds.isEmpty();
    }

    public int getSize() {
        return mTrackIds.size();
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

    public PlaySourceInfo getPlaySourceInfo() {
        return mPlaySourceInfo;
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
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mTrackIds.size());
        dest.writeLongArray(Longs.toArray(mTrackIds));
        dest.writeInt(mPlayPosition);
        dest.writeString(mAppendState.name());
        dest.writeBundle(mPlaySourceInfo.getData());
    }

    public static final Parcelable.Creator<PlayQueueState> CREATOR = new Parcelable.Creator<PlayQueueState>() {
        public PlayQueueState createFromParcel(Parcel in) {
            return new PlayQueueState(in);
        }

        public PlayQueueState[] newArray(int size) {
            return new PlayQueueState[size];
        }
    };

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("Track IDs", mTrackIds)
                .add("Size", getSize())
                .add("Play Position", mPlayPosition)
                .add("Append State", mAppendState)
                .toString();
    }
}
