package com.soundcloud.android.tracking.eventlogger;

import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.utils.ScTextUtils;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class PlaySessionSource implements Parcelable{

    public static PlaySessionSource EMPTY = new PlaySessionSource();

    public enum DiscoverySource {
        RECOMMENDER, EXPLORE;

        public String value() {
            return this.toString().toLowerCase();
        }
    }

    private final Uri mOriginPage;
    private final long mSetId;
    private String mExploreVersion;

    public PlaySessionSource(Parcel in) {
        mSetId = in.readLong();
        mOriginPage = in.readParcelable(PlaySessionSource.class.getClassLoader());
        mExploreVersion = in.readString();
    }

    public PlaySessionSource() {
        this(Uri.EMPTY);
    }

    public PlaySessionSource(Uri originPage) {
        this(originPage, ScModel.NOT_SET);
    }

    public PlaySessionSource(Uri originPage, long setId) {
        mOriginPage = originPage;
        mSetId = setId;
    }

    public PlaySessionSource(Uri originPage, String exploreVersion) {
        this(originPage, ScModel.NOT_SET, exploreVersion);
    }

    public PlaySessionSource(Uri originPage, long setId, String exploreVersion) {
        mOriginPage = originPage;
        mSetId = setId;
        mExploreVersion = exploreVersion;
    }

    public Uri getOriginPage() {
        return mOriginPage;
    }

    public long getSetId() {
        return mSetId;
    }

    public String getInitialSource() {
        return ScTextUtils.isNotBlank(mExploreVersion) ? DiscoverySource.EXPLORE.value() : ScTextUtils.EMPTY_STRING;
    }

    public String getInitialSourceVersion() {
        return ScTextUtils.isNotBlank(mExploreVersion) ? mExploreVersion : ScTextUtils.EMPTY_STRING;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mSetId);
        dest.writeParcelable(mOriginPage, 0);
        dest.writeString(mExploreVersion);
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
        if (o == null || getClass() != o.getClass()) return false;

        PlaySessionSource that = (PlaySessionSource) o;

        if (mSetId != that.mSetId) return false;
        if (mExploreVersion != null ? !mExploreVersion.equals(that.mExploreVersion) : that.mExploreVersion != null)
            return false;
        if (!mOriginPage.equals(that.mOriginPage)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mOriginPage.hashCode();
        result = 31 * result + (int) (mSetId ^ (mSetId >>> 32));
        result = 31 * result + (mExploreVersion != null ? mExploreVersion.hashCode() : 0);
        return result;
    }
}
