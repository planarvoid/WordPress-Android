package com.soundcloud.android.tracking.eventlogger;

import com.google.common.base.Objects;
import com.google.common.primitives.Longs;
import com.soundcloud.android.utils.ScTextUtils;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Used to track the origin of play events. This will eventually be passed to {@link PlayEventTracker}
 */
public class PlaySourceInfo implements Parcelable {

    public static final PlaySourceInfo empty(){
        return new PlaySourceInfo();
    }

    private static final String KEY_ORIGIN_URL = "playSource-originUrl";
    private static final String KEY_EXPLORE_TAG = "playSource-exploreTag";
    private static final String KEY_RECOMMENDER_VERSION = "playSource-recommenderVersion";

    private Bundle mData;

    public PlaySourceInfo(Bundle data) {
        mData = data;
    }

    private PlaySourceInfo() {
        this(new Bundle());
    }

    private PlaySourceInfo(Builder builder) {
        this();
        if (ScTextUtils.isNotBlank(builder.mOriginUrl)) mData.putString(KEY_ORIGIN_URL, builder.mOriginUrl);
        if (ScTextUtils.isNotBlank(builder.mExploreTag)) mData.putString(KEY_EXPLORE_TAG, builder.mExploreTag);
        if (ScTextUtils.isNotBlank(builder.mRecommenderVersion)) mData.putString(KEY_RECOMMENDER_VERSION, builder.mRecommenderVersion);
    }

    public PlaySourceInfo(Parcel parcel) {
        mData = parcel.readBundle();
    }

    /**
     * This exist purely for the PlayQueueUri to persist and recreate tracking through app lifecycles
     * {@link com.soundcloud.android.service.playback.PlayQueueUri}
     */
    public static PlaySourceInfo fromUriParams(Uri uri) {
        return new Builder()
                .originUrl(uri.getQueryParameter(KEY_ORIGIN_URL))
                .exploreTag(uri.getQueryParameter(KEY_EXPLORE_TAG))
                .recommenderVersion(uri.getQueryParameter(KEY_RECOMMENDER_VERSION))
                .build();
    }

    public void setRecommenderVersion(String version) {
        mData.putString(KEY_RECOMMENDER_VERSION, version);
    }

    public String getRecommenderVersion() {
        return mData.getString(KEY_RECOMMENDER_VERSION);
    }

    public String getExploreTag() {
        return mData.getString(KEY_EXPLORE_TAG);
    }

    public Bundle getData(){
        return mData;
    }

    public Uri.Builder appendAsQueryParams(Uri.Builder builder) {
        for (String key : mData.keySet()) {
            builder.appendQueryParameter(key, String.valueOf(mData.get(key)));
        }
        return builder;
    }

    public Uri.Builder appendEventLoggerParams(Uri.Builder builder) {
        final String originUrl = mData.getString(KEY_ORIGIN_URL);
        if (ScTextUtils.isNotBlank(originUrl)){
            builder.appendQueryParameter(PlayEventTracker.EventLoggerKeys.ORIGIN_URL, originUrl);
        }

        final String exploreTag = mData.getString(KEY_EXPLORE_TAG);
        if (ScTextUtils.isNotBlank(exploreTag)){
            builder.appendQueryParameter(PlayEventTracker.EventLoggerKeys.EXPLORE_TAG, exploreTag);
        }
        return builder;
    }

    /**
     * WARNING : This makes a lot of assumptions about what is possible with the current system of playback, namely that
     * explore will only have 1 manually triggered track, and the entire rest of the list will be recommended
     * @return the proper TrackSourceInfo based on the given
     */
    public TrackSourceInfo getAutoTrackSource() {
        if (getRecommenderVersion() != null) {
            return TrackSourceInfo.fromRecommender(getRecommenderVersion());
        }
        return TrackSourceInfo.auto();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(mData);
    }

    public static final Parcelable.Creator<PlaySourceInfo> CREATOR = new Parcelable.Creator<PlaySourceInfo>() {
        public PlaySourceInfo createFromParcel(Parcel in) {
            return new PlaySourceInfo(in);
        }

        public PlaySourceInfo[] newArray(int size) {
            return new PlaySourceInfo[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equal(mData, ((PlaySourceInfo) o).mData);
    }

    @Override
    public int hashCode() {
        return mData.hashCode();
    }

    @Override
    public String toString() {
        final Objects.ToStringHelper toStringHelper = Objects.toStringHelper(getClass());
        for (String key : mData.keySet()){
            toStringHelper.add(key, mData.get(key));
        }
        return toStringHelper.toString();
    }

    public static class Builder {
        private String mOriginUrl;
        private String mExploreTag;
        private String mRecommenderVersion;

        public Builder() {}

        public Builder originUrl(String originUrl) {
            mOriginUrl = originUrl;
            return this;
        }

        public Builder exploreTag(String exploreTag) {
            mExploreTag = exploreTag;
            return this;
        }

        public Builder recommenderVersion(String recommenderVersion) {
            mRecommenderVersion = recommenderVersion;
            return this;
        }

        public PlaySourceInfo build() {
            return new PlaySourceInfo(this);
        }
    }
}
