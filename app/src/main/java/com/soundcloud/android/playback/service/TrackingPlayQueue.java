package com.soundcloud.android.playback.service;

import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;
import com.soundcloud.android.tracking.eventlogger.TrackSourceInfo;
import com.soundcloud.android.utils.ScTextUtils;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.List;

class TrackingPlayQueue extends PlayQueue {
    public static final TrackingPlayQueue EMPTY = new TrackingPlayQueue(Collections.<Long>emptyList(), -1,PlaySourceInfo.empty());
    private boolean mCurrentTrackIsUserTriggered;


    private PlaySourceInfo mPlaySourceInfo = PlaySourceInfo.empty();
    private Uri mSourceUri = Uri.EMPTY; // just for "back to set" functionality in the Action Bar

    public TrackingPlayQueue(Long id) {
        super(id);
    }

    public TrackingPlayQueue(Parcel in) {
        super(in);

        mCurrentTrackIsUserTriggered = in.readInt() == 1;
        mSourceUri = in.readParcelable(Uri.class.getClassLoader());
        mPlaySourceInfo = new PlaySourceInfo(in.readBundle());
    }

    public TrackingPlayQueue(List<Long> trackIds, int playPosition) {
        super(trackIds, playPosition);
    }

    public TrackingPlayQueue(List<Long> trackIds, int playPosition, PlaySourceInfo playSourceInfo) {
        super(trackIds, playPosition);
        mPlaySourceInfo = playSourceInfo;
    }

    public TrackingPlayQueue(List<Long> currentTrackIds, int playPosition, PlaySourceInfo playSourceInfo, Uri sourceUri) {
        this(currentTrackIds, playPosition, playSourceInfo);
        mSourceUri = sourceUri;
    }

    public Uri getSourceUri() {
        return mSourceUri;
    }

    public PlaySourceInfo getPlaySourceInfo() {
        return mPlaySourceInfo;
    }

    public void setCurrentTrackToUserTriggered() {
        mCurrentTrackIsUserTriggered = true;
    }

    /* package */ Uri getPlayQueueState(long seekPos, long currentTrackId) {
        return new PlayQueueUri().toUri(currentTrackId, mPosition, seekPos, mPlaySourceInfo);
    }

    public boolean moveToPrevious() {
        boolean changed = super.moveToPrevious();
        if (changed) {
            mCurrentTrackIsUserTriggered = true;
        }
        return changed;
    }

    public boolean moveToNext(boolean userTriggered) {
        boolean changed = super.moveToNext();
        if (changed){
            mCurrentTrackIsUserTriggered = userTriggered;
        }
        return changed;
    }

    public String getCurrentEventLoggerParams() {
        if (isEmpty()) return ScTextUtils.EMPTY_STRING;

        final TrackSourceInfo trackSourceInfo = mPlaySourceInfo.getTrackSource(getCurrentTrackId());
        trackSourceInfo.setTrigger(mCurrentTrackIsUserTriggered);

        if (mSourceUri != null && Content.match(mSourceUri) == Content.PLAYLIST) {
            return trackSourceInfo.createEventLoggerParamsForSet(mSourceUri.getLastPathSegment(), String.valueOf(mPosition));
        } else {
            return trackSourceInfo.createEventLoggerParams();
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mCurrentTrackIsUserTriggered ? 1 : 0);
        dest.writeParcelable(mSourceUri, 0);
        dest.writeBundle(mPlaySourceInfo.getData());
    }

    public static final Parcelable.Creator<TrackingPlayQueue> CREATOR = new Parcelable.Creator<TrackingPlayQueue>() {
        public TrackingPlayQueue createFromParcel(Parcel in) {
            return new TrackingPlayQueue(in);
        }

        public TrackingPlayQueue[] newArray(int size) {
            return new TrackingPlayQueue[size];
        }
    };
}
