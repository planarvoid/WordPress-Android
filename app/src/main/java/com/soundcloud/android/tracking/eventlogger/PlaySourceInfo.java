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
    private static final String KEY_INITIAL_TRACK_ID = "playSource-initialTrackId";
    private static final String KEY_EXPLORE_VERSION = "playSource-exploreVersion";
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
        if (builder.mInitialTrackId > 0) mData.putLong(KEY_INITIAL_TRACK_ID, builder.mInitialTrackId);
        if (ScTextUtils.isNotBlank(builder.mOriginUrl)) mData.putString(KEY_ORIGIN_URL, builder.mOriginUrl);
        if (ScTextUtils.isNotBlank(builder.mExploreVersion)) mData.putString(KEY_EXPLORE_VERSION, builder.mExploreVersion);
        if (ScTextUtils.isNotBlank(builder.mRecommenderVersion)) mData.putString(KEY_RECOMMENDER_VERSION, builder.mRecommenderVersion);
    }

    public PlaySourceInfo(Parcel parcel) {
        mData = parcel.readBundle();
    }

    /**
     * This exist purely for the PlayQueueUri to persist and recreate tracking through app lifecycles
     * {@link com.soundcloud.android.playback.service.PlayQueueUri}
     */
    public static PlaySourceInfo fromUriParams(Uri uri) {
        final Builder builder = new Builder();
        final String initialTrackId = uri.getQueryParameter(KEY_INITIAL_TRACK_ID);
        if (ScTextUtils.isNotBlank(initialTrackId)){
            builder.initialTrackId(Longs.tryParse(initialTrackId));
        }
        return builder
                .originUrl(uri.getQueryParameter(KEY_ORIGIN_URL))
                .exploreVersion(uri.getQueryParameter(KEY_EXPLORE_VERSION))
                .recommenderVersion(uri.getQueryParameter(KEY_RECOMMENDER_VERSION))
                .build();
    }

    public void setRecommenderVersion(String version) {
        mData.putString(KEY_RECOMMENDER_VERSION, version);
    }

    public String getRecommenderVersion() {
        return mData.getString(KEY_RECOMMENDER_VERSION);
    }

    public Long getInitialTrackId() {
        return mData.getLong(KEY_INITIAL_TRACK_ID);
    }

    public String getExploreTag() {
        return mData.getString(KEY_EXPLORE_VERSION);
    }

    public String getOriginUrl() {
        return mData.getString(KEY_ORIGIN_URL);
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

    /**
     * WARNING : This makes a lot of assumptions about what is possible with the current system of playback, namely that
     * we have unique track ids in a given tracklist, so you can always attribute the source properly
     * @return the proper TrackSourceInfo based on the given
     * @param trackId the currently playing track
     */
    public TrackSourceInfo getTrackSource(long trackId) {
        if (trackId == getInitialTrackId()){
            if (getExploreTag() != null){
                return TrackSourceInfo.fromExplore(getExploreTag(), getOriginUrl());
            }
        } else {
            if (getRecommenderVersion() != null) {
                return TrackSourceInfo.fromRecommender(getRecommenderVersion(), getOriginUrl());
            }
        }
        return new TrackSourceInfo();
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
        private long mInitialTrackId;
        private String mOriginUrl;
        private String mExploreVersion;
        private String mRecommenderVersion;

        public Builder() {}

        public Builder initialTrackId(long initialTrackId) {
            mInitialTrackId = initialTrackId;
            return this;
        }

        public Builder originUrl(String originUrl) {
            mOriginUrl = originUrl;
            return this;
        }

        public Builder exploreVersion(String exploreVersion) {
            mExploreVersion = exploreVersion;
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
