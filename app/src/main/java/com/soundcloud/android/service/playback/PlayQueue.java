package com.soundcloud.android.service.playback;


import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;
import com.soundcloud.android.tracking.eventlogger.TrackSourceInfo;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PlayQueue implements Parcelable, Iterable<Long> {
    public static final String EXTRA = "PlayQueue";
    public static final PlayQueue EMPTY = new PlayQueue(Collections.<Long>emptyList(), -1, PlaySourceInfo.EMPTY);

    private int mPosition;
    private List<Long> mTrackIds = Collections.emptyList();
    private AppendState mAppendState = AppendState.IDLE;
    private PlaySourceInfo mPlaySourceInfo = PlaySourceInfo.EMPTY;
    private Uri mSourceUri; // just for "back to set" functionality in the Action Bar

    public enum AppendState {
        IDLE, LOADING, ERROR, EMPTY;
    }

    public PlayQueue(Long id) {
        mTrackIds = Lists.newArrayList(id);
        mPosition = 0;
    }

    public PlayQueue(List<Long> trackIds, int playPosition) {
        mTrackIds = trackIds;
        mPosition = playPosition;
    }

    public PlayQueue(List<Long> trackIds, int playPosition, PlaySourceInfo playSourceInfo) {
        this(trackIds, playPosition);
        mPlaySourceInfo = playSourceInfo;
    }

    public PlayQueue(List<Long> currentTrackIds, int playPosition, AppendState appendState) {
        this(currentTrackIds, playPosition, PlaySourceInfo.EMPTY);
        mAppendState = appendState;
    }

    public PlayQueue(Parcel in) {
        final int size = in.readInt();
        long[] trackIds = new long[size];
        in.readLongArray(trackIds);

        mTrackIds = Lists.newArrayListWithExpectedSize(trackIds.length);
        for (long n : trackIds) mTrackIds.add(n);

        mPosition = in.readInt();
        mAppendState = AppendState.valueOf(in.readString());
        mPlaySourceInfo = new PlaySourceInfo(in.readBundle());
    }

    @Override
    public Iterator<Long> iterator() {
        return mTrackIds.iterator();
    }

    /* package */ void setAppendState(AppendState appendState) {
        mAppendState = appendState;
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

    public void addTrackId(long id) {
        mTrackIds.add(id);
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

    // TODO, set this to the source URI
    public void setSourceUri(Uri uri) {
        mSourceUri = uri;
    }

    public Uri getSourceUri() {
        return mSourceUri;
    }

    public PlaySourceInfo getPlaySourceInfo() {
        return mPlaySourceInfo;
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

    /**
     * TODO : We need to figure out how to decouple event logger params from the playqueue
     */
    public String getCurrentEventLoggerParams() {
        final TrackSourceInfo trackSourceInfo = mPlaySourceInfo.getTrackSourceById(getCurrentTrackId());
        return trackSourceInfo.createEventLoggerParams(mPlaySourceInfo);
    }

    /* package */ Uri getPlayQueueState(long seekPos, long currentTrackId) {
        return new PlayQueueUri().toUri(currentTrackId, mPosition, seekPos, mPlaySourceInfo);
    }

    public boolean moveToPrevious() {
        if (mPosition > 0) {
            mPosition--;
            return true;
        }
        return false;
    }

    public boolean moveToNext() {
        if (!isLastTrack()) {
            mPosition++;
            return true;
        }
        return false;
    }

    public boolean isLastTrack() {
        return mPosition >= mTrackIds.size() - 1;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mTrackIds.size());
        dest.writeLongArray(Longs.toArray(mTrackIds));
        dest.writeInt(mPosition);
        dest.writeString(mAppendState.name());
        dest.writeBundle(mPlaySourceInfo.getData());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<PlayQueue> CREATOR = new Parcelable.Creator<PlayQueue>() {
        public PlayQueue createFromParcel(Parcel in) {
            return new PlayQueue(in);
        }

        public PlayQueue[] newArray(int size) {
            return new PlayQueue[size];
        }
    };

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("Track IDs", mTrackIds)
                .add("Size", size())
                .add("Play Position", mPosition)
                .add("Append State", mAppendState)
                .toString();
    }

}
