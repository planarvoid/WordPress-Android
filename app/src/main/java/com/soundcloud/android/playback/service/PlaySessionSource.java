package com.soundcloud.android.playback.service;

import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.tracking.eventlogger.TrackSourceInfo;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class PlaySessionSource implements Parcelable{

    public static PlaySessionSource EMPTY = new PlaySessionSource();

    private final Uri mOriginPage;
    private final long mSetId;
    private final TrackSourceInfo mTrackSourceInfo;

    public PlaySessionSource(Parcel in) {
        mSetId = in.readLong();
        mOriginPage = in.readParcelable(PlaySessionSource.class.getClassLoader());
        mTrackSourceInfo = TrackSourceInfo.fromSource(in.readString(), in.readString());
    }

    public PlaySessionSource() {
        this(Uri.EMPTY);
    }

    public PlaySessionSource(Uri originPage) {
        this(originPage, ScModel.NOT_SET);
    }

    public PlaySessionSource(Uri originPage, long setId) {
        this(originPage, TrackSourceInfo.EMPTY, setId);
    }

    public PlaySessionSource(Uri originPage, TrackSourceInfo trackSourceInfo) {
        this(originPage, trackSourceInfo, ScModel.NOT_SET);
    }

    public PlaySessionSource(Uri originPage, TrackSourceInfo trackSourceInfo, long setId) {
        mOriginPage = originPage;
        mSetId = setId;
        mTrackSourceInfo = trackSourceInfo;
    }

    Uri getOriginPage() {
        return mOriginPage;
    }

    long getSetId() {
        return mSetId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mSetId);
        dest.writeParcelable(mOriginPage, 0);
        dest.writeString(mTrackSourceInfo.getSource());
        dest.writeString(mTrackSourceInfo.getSourceVersion());
    }

    public static final Parcelable.Creator<PlaySessionSource> CREATOR = new Parcelable.Creator<PlaySessionSource>() {
        public PlaySessionSource createFromParcel(Parcel in) {
            return new PlaySessionSource(in);
        }

        public PlaySessionSource[] newArray(int size) {
            return new PlaySessionSource[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlaySessionSource)) return false;

        PlaySessionSource that = (PlaySessionSource) o;

        if (mSetId != that.mSetId) return false;
        if (!mOriginPage.equals(that.mOriginPage)) return false;
        if (!mTrackSourceInfo.equals(that.mTrackSourceInfo)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mOriginPage.hashCode();
        result = 31 * result + (int) (mSetId ^ (mSetId >>> 32));
        result = 31 * result + mTrackSourceInfo.hashCode();
        return result;
    }
}
