package com.soundcloud.android.service.playback;


import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;
import com.soundcloud.android.tracking.eventlogger.TrackSourceInfo;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PlayQueue implements Parcelable, Iterable<Long> {
    public static final String EXTRA = "PlayQueue";
    public static final PlayQueue EMPTY = new PlayQueue(Collections.<Long>emptyList(), -1, PlaySourceInfo.empty());

    private int mPosition;
    private boolean mCurrentTrackIsUserTriggered;
    private List<Long> mTrackIds = Collections.emptyList();
    private AppendState mAppendState = AppendState.IDLE;
    private PlaySourceInfo mPlaySourceInfo = PlaySourceInfo.empty();
    private Uri mSourceUri = Uri.EMPTY; // just for "back to set" functionality in the Action Bar

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

    public PlayQueue(List<Long> currentTrackIds, int playPosition, PlaySourceInfo playSourceInfo, Uri sourceUri) {
        this(currentTrackIds, playPosition, playSourceInfo);
        mSourceUri = sourceUri;
    }

    @VisibleForTesting
    public PlayQueue(ArrayList<Long> trackIds, int playPosition, AppendState appendState) {
        this(trackIds, playPosition, PlaySourceInfo.empty());
        mAppendState = appendState;
    }

    public PlayQueue(Parcel in) {
        final int size = in.readInt();
        long[] trackIds = new long[size];
        in.readLongArray(trackIds);

        mTrackIds = Lists.newArrayListWithExpectedSize(trackIds.length);
        for (long n : trackIds) mTrackIds.add(n);

        mPosition = in.readInt();
        mCurrentTrackIsUserTriggered = in.readInt() == 1;
        mAppendState = AppendState.valueOf(in.readString());
        mSourceUri = in.readParcelable(Uri.class.getClassLoader());
        mPlaySourceInfo = new PlaySourceInfo(in.readBundle());
    }

    @Override
    public Iterator<Long> iterator() {
        return mTrackIds.iterator();
    }

    public void setCurrentTrackToUserTriggered() {
        mCurrentTrackIsUserTriggered = true;
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
            mCurrentTrackIsUserTriggered = true;
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

    public String getCurrentEventLoggerParams() {
        final TrackSourceInfo trackSourceInfo = mPlaySourceInfo.getTrackSource(getCurrentTrackId());
        trackSourceInfo.setTrigger(mCurrentTrackIsUserTriggered);

        if (mSourceUri != null && Content.match(mSourceUri) == Content.PLAYLIST) {
            return trackSourceInfo.createEventLoggerParamsForSet(mSourceUri.getLastPathSegment(), String.valueOf(mPosition));
        } else {
            return trackSourceInfo.createEventLoggerParams();
        }
    }

    /* package */ Uri getPlayQueueState(long seekPos, long currentTrackId) {
        return new PlayQueueUri().toUri(currentTrackId, mPosition, seekPos, mPlaySourceInfo);
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
        if (!isLastTrack()) {
            mPosition++;
            mCurrentTrackIsUserTriggered = userTriggered;
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
        dest.writeInt(mCurrentTrackIsUserTriggered ? 1 : 0);
        dest.writeString(mAppendState.name());
        dest.writeParcelable(mSourceUri, 0);
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
