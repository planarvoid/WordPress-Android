package com.soundcloud.android.playback.service;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.utils.ScTextUtils;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Locale;

public class PlaySessionSource implements Parcelable{

    public static final PlaySessionSource EMPTY = new PlaySessionSource();

    public enum DiscoverySource {
        RECOMMENDER, EXPLORE;

        public String value() {
            return this.toString().toLowerCase(Locale.ENGLISH);
        }
    }

    private final String mOriginPage;
    private final long mSetId;
    private String mExploreVersion;

    public PlaySessionSource(Parcel in) {
        mSetId = in.readLong();
        mOriginPage = in.readString();
        mExploreVersion = in.readString();
    }

    public PlaySessionSource() {
        this(ScTextUtils.EMPTY_STRING);
    }

    public PlaySessionSource(Screen screen) {
        this(screen.get());
    }

    public PlaySessionSource(String originScreen) {
        this(originScreen, ScModel.NOT_SET);
    }

    public PlaySessionSource(String originScreen, long setId) {
        mOriginPage = originScreen;
        mSetId = setId;
    }

    public PlaySessionSource(String originScreen, String exploreVersion) {
        this(originScreen, ScModel.NOT_SET, exploreVersion);
    }

    public PlaySessionSource(String originScreen, long setId, String exploreVersion) {
        mOriginPage = originScreen;
        mSetId = setId;
        mExploreVersion = exploreVersion;
    }

    public String getOriginPage() {
        return mOriginPage;
    }

    public long getSetId() {
        return mSetId;
    }

    // TODO, finalize this once we implement page tracking
    public boolean originatedInExplore(){
        return mOriginPage.startsWith("explore");
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
        dest.writeString(mOriginPage);
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
