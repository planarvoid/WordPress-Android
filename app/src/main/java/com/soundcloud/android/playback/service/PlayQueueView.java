package com.soundcloud.android.playback.service;


import static com.soundcloud.android.playback.PlaybackOperations.AppendState;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PlayQueueView implements Parcelable, Iterable<Long> {
    public static final String EXTRA = "PlayQueue";
    public static final PlayQueueView EMPTY = new PlayQueueView(Collections.<Long>emptyList(), -1);

    private final List<Long> mTrackIds;
    private final AppendState mAppendState;
    private int mPosition;

    public PlayQueueView(Long id) {
        this(Lists.newArrayList(id), 0);
    }

    public PlayQueueView(List<Long> trackIds, int playPosition) {
        this(trackIds, playPosition, AppendState.IDLE);
    }

    public PlayQueueView(List<Long> trackIds, int playPosition, AppendState appendState) {
        mTrackIds = ImmutableList.copyOf(trackIds);
        mPosition = playPosition < 0 || playPosition >= trackIds.size() ? 0 : playPosition;
        mAppendState = appendState;
    }

    private PlayQueueView(Parcel in) {
        final int size = in.readInt();
        long[] trackIds = new long[size];
        in.readLongArray(trackIds);

        mTrackIds = Lists.newArrayListWithExpectedSize(trackIds.length);
        for (long n : trackIds) mTrackIds.add(n);
        mPosition = in.readInt();
        mAppendState = AppendState.valueOf(in.readString());

    }

    @Override
    public Iterator<Long> iterator() {
        return mTrackIds.iterator();
    }

    public AppendState getAppendState() {
        return mAppendState;
    }

    public boolean isEmpty() {
        return mTrackIds.isEmpty();
    }

    public int size() {
        return mTrackIds.size();
    }

    public int getPosition() {
        return mPosition;
    }

    public boolean setPosition(int position) {
        if (position < mTrackIds.size()) {
            mPosition = position;
            return true;
        } else {
            return false;
        }
    }

    public long getCurrentTrackId() {
        return getTrackIdAt(mPosition);
    }

    public long getTrackIdAt(int position) {
        return mTrackIds.get(position);
    }

    public int getPositionOfTrackId(long trackId) {
        return mTrackIds.indexOf(trackId);
    }

    public boolean isLastTrack() {
        return mPosition >= mTrackIds.size() - 1;
    }

    public boolean isLoading() {
        return mAppendState == AppendState.LOADING;
    }

    public boolean lastLoadFailed() {
        return mAppendState == AppendState.ERROR;
    }

    public boolean lastLoadWasEmpty() {
        return mAppendState == AppendState.EMPTY;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mTrackIds.size());
        dest.writeLongArray(Longs.toArray(mTrackIds));
        dest.writeInt(mPosition);
        dest.writeString(mAppendState.name());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<PlayQueueView> CREATOR = new Parcelable.Creator<PlayQueueView>() {
        public PlayQueueView createFromParcel(Parcel in) {
            return new PlayQueueView(in);
        }

        public PlayQueueView[] newArray(int size) {
            return new PlayQueueView[size];
        }
    };

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("Track IDs", mTrackIds)
                .add("Size", size())
                .add("Play Position", mPosition)
                .toString();
    }
}
