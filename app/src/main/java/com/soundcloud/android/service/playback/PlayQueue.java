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
import java.util.List;

public class PlayQueue implements Parcelable {
    public static final String EXTRA = "PlayQueue";
    public static final PlayQueue EMPTY = new PlayQueue(Collections.<Long>emptyList(), -1, PlaySourceInfo.EMPTY);

    private int mPlayPosition;
    private List<Long> mTrackIds = Collections.emptyList();
    private AppendState mAppendState = AppendState.IDLE;
    private PlaySourceInfo mPlaySourceInfo = PlaySourceInfo.EMPTY;
    private Uri mSourceUri; // just for "back to set" functionality in the Action Bar

    public enum AppendState {
        IDLE, LOADING, ERROR, EMPTY;
    }

    public PlayQueue(Long id) {
        mTrackIds = Lists.newArrayList(id);
        mPlayPosition = 0;
    }

    public PlayQueue(List<Long> trackIds, int playPosition) {
        mTrackIds = trackIds;
        mPlayPosition = playPosition;
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

        mPlayPosition = in.readInt();
        mAppendState = AppendState.valueOf(in.readString());
        mPlaySourceInfo = new PlaySourceInfo(in.readBundle());
    }

    /* package */ void setAppendState(AppendState appendState) {
        mAppendState = appendState;
    }

    public AppendState getAppendState() {
        return mAppendState;
    }

    public void addTrack(long id) {
        mTrackIds.add(id);
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

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * TODO : We need to figure out how to decouple event logger params from the playqueue
     */
    public String getCurrentEventLoggerParams() {
        final TrackSourceInfo trackSourceInfo = mPlaySourceInfo.getTrackSourceById(getCurrentTrackId());
        return trackSourceInfo.createEventLoggerParams(mPlaySourceInfo);
    }

    /* package */ Uri getPlayQueueState(long seekPos, long currentTrackId) {
        return new PlayQueueUri().toUri(currentTrackId, mPlayPosition, seekPos, mPlaySourceInfo);
    }

    public List<Long> getTrackIds() {
        return mTrackIds;
    }

    public int length() {
        return mTrackIds.size();
    }

    // TODO, set this to the source URI
    public void setSourceUri(Uri uri){
        mSourceUri = uri;
    }

    public Uri getSourceUri(){
        return mSourceUri;
    }

    public boolean setPosition(int playPos) {
        if (playPos < mTrackIds.size()) {
            mPlayPosition = playPos;
            return true;
        } else {
            return false;
        }
    }

    public long getCurrentTrackId() {
        return getTrackIdAt(mPlayPosition);
    }

    public long getTrackIdAt(int playPos){
        return mTrackIds.get(playPos);
    }

    public boolean prev() {
        if (mPlayPosition > 0) {
            mPlayPosition--;
            return true;
        }
        return false;
    }

    public Boolean next() {
        if (!onLastTrack()) {
            mPlayPosition++;
            return true;
        }
        return false;
    }

    public boolean onLastTrack(){
        return mPlayPosition < mTrackIds.size() - 1;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mTrackIds.size());
        dest.writeLongArray(Longs.toArray(mTrackIds));
        dest.writeInt(mPlayPosition);
        dest.writeString(mAppendState.name());
        dest.writeBundle(mPlaySourceInfo.getData());
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
                .add("Size", getSize())
                .add("Play Position", mPlayPosition)
                .add("Append State", mAppendState)
                .toString();
    }

}
