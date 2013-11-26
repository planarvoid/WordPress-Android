package com.soundcloud.android.playback.service;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.tracking.eventlogger.TrackSourceInfo;
import com.soundcloud.android.utils.ScTextUtils;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

class TrackingPlayQueue extends PlayQueue {

    // TODO, get rid of this, it is mutable
    public static final TrackingPlayQueue EMPTY = new TrackingPlayQueue(Collections.<Long>emptyList(), -1,
            PlaySessionSource.EMPTY, TrackSourceInfo.EMPTY);

    private boolean mCurrentTrackIsUserTriggered;
    private TrackSourceInfoMap mTrackSourceInfoMap;
    private PlaySessionSource mPlaySessionSource;

    public TrackingPlayQueue(Parcel in) {
        super(in);
        mCurrentTrackIsUserTriggered = in.readInt() == 1;
        mPlaySessionSource = in.readParcelable(PlaySessionSource.class.getClassLoader());
        //mOriginUrl = in.readString();
    }

    public TrackingPlayQueue(List<Long> trackIds, int playPosition, PlaySessionSource playSessionSource) {
        super(trackIds, playPosition);
        mPlaySessionSource = playSessionSource;
    }

    public TrackingPlayQueue(List<Long> currentTrackIds, int startPosition,
                             PlaySessionSource playSessionSource, TrackSourceInfo initialTrackSourceInfo) {
        this(currentTrackIds, startPosition, playSessionSource);
        mTrackSourceInfoMap = new TrackSourceInfoMap(currentTrackIds, initialTrackSourceInfo);
    }

    @VisibleForTesting
    TrackingPlayQueue(ArrayList<Long> trackIds, int position) {
        super(trackIds, position);
    }

    @VisibleForTesting
    TrackingPlayQueue(long trackId) {
        super(trackId);
    }

    public Uri getOriginPage() {
        return mPlaySessionSource.getOriginPage();
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
        super.addTrackId(id);
        mTrackSourceInfoMap.put(id, trackSourceInfo);
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
        if (changed) {
            mCurrentTrackIsUserTriggered = userTriggered;
        }
        return changed;
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

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mCurrentTrackIsUserTriggered ? 1 : 0);
        dest.writeParcelable(mPlaySessionSource, 0);
    }

    public static final Parcelable.Creator<TrackingPlayQueue> CREATOR = new Parcelable.Creator<TrackingPlayQueue>() {
        public TrackingPlayQueue createFromParcel(Parcel in) {
            return new TrackingPlayQueue(in);
        }

        public TrackingPlayQueue[] newArray(int size) {
            return new TrackingPlayQueue[size];
        }
    };

    private static class TrackSourceInfoMap extends HashMap<Long, TrackSourceInfo> {
        public TrackSourceInfoMap(List<Long> trackIds, TrackSourceInfo trackSourceInfo) {
            super(trackIds.size());
            for (Long l : trackIds){
                put(l, trackSourceInfo);
            }
        }

        public TrackSourceInfo get(Long key, TrackSourceInfo defaultValue) {
            TrackSourceInfo ret = get(key);
            if (ret == null) {
                return TrackSourceInfo.EMPTY;
            }
            return ret;
        }
    }
}
