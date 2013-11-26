package com.soundcloud.android.playback.service;

import com.soundcloud.android.model.ScModel;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

class PlaySessionSource implements Parcelable{

    public static PlaySessionSource EMPTY = new PlaySessionSource();

    private final Uri mOriginPage;
    private final long mSetId;

    public PlaySessionSource() {
        this(Uri.EMPTY);
    }

    PlaySessionSource(Uri originPage) {
        this(originPage, ScModel.NOT_SET);
    }

    PlaySessionSource(Uri originPage, long setId) {
        mOriginPage = originPage;
        mSetId = setId;
    }

    public PlaySessionSource(Parcel in) {
        mSetId = in.readLong();
        mOriginPage = in.readParcelable(PlaySessionSource.class.getClassLoader());
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
    }

    public static final Parcelable.Creator<PlaySessionSource> CREATOR = new Parcelable.Creator<PlaySessionSource>() {
        public PlaySessionSource createFromParcel(Parcel in) {
            return new PlaySessionSource(in);
        }

        public PlaySessionSource[] newArray(int size) {
            return new PlaySessionSource[size];
        }
    };
}
