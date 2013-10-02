package com.soundcloud.android.tracking.eventlogger;

import com.google.common.primitives.Longs;
import com.soundcloud.android.utils.ScTextUtils;

import android.net.Uri;

public class PlaySourceInfo extends EventLoggerParamsMap {

    public static final PlaySourceInfo EMPTY = new PlaySourceInfo(0);

    private static final String KEY_ORIGIN_URL = "playSource-originUrl";
    private static final String KEY_EXPLORE_TAG = "playSource-exploreTag";
    private static final String KEY_INITIAL_TRACK_ID = "playSource-initialTrackId";
    private static final String KEY_RECOMMENDER_VERSION = "playSource-recommenderVersion";

    public PlaySourceInfo(int capacity) {
        super(capacity);
    }

    private PlaySourceInfo(Builder builder) {
        super(4); // expect capacity set to key count. Up this if we add more keys
        put(KEY_INITIAL_TRACK_ID, String.valueOf(builder.mInitialTrackId));
        if (ScTextUtils.isNotBlank(builder.mOriginUrl)) put(KEY_ORIGIN_URL, builder.mOriginUrl);
        if (ScTextUtils.isNotBlank(builder.mExploreTag)) put(KEY_EXPLORE_TAG, builder.mExploreTag);
        if (ScTextUtils.isNotBlank(builder.mRecommenderVersion)) put(KEY_RECOMMENDER_VERSION, builder.mRecommenderVersion);

    }

    /**
     * This exist purely for the PlayQueueUri to persist and recreate tracking through app lifecycles
     * {@link com.soundcloud.android.service.playback.PlayQueueUri}
     */
    public static PlaySourceInfo fromUriParams(Uri uri) {
        return new PlaySourceInfo(
                new Builder(Longs.tryParse(uri.getQueryParameter(KEY_INITIAL_TRACK_ID)))
                        .originUrl(uri.getQueryParameter(KEY_ORIGIN_URL))
                        .exploreTag(uri.getQueryParameter(KEY_EXPLORE_TAG))
                        .recommenderVersion(uri.getQueryParameter(KEY_RECOMMENDER_VERSION))
        );
    }

    public void setInitialTrackId(long id){
        put(KEY_INITIAL_TRACK_ID, String.valueOf(id));
    }

    public void setRecommenderVersion(String version) {
        put(KEY_RECOMMENDER_VERSION, version);
    }

    public String getRecommenderVersion() {
        return get(KEY_RECOMMENDER_VERSION);
    }

    public Long getInitialTrackId() {
        return Longs.tryParse(get(KEY_INITIAL_TRACK_ID));
    }

    @Override
    public Uri.Builder appendEventLoggerParams(Uri.Builder builder) {
        builder.appendQueryParameter(ExternalKeys.ORIGIN_URL, get(KEY_ORIGIN_URL));
        builder.appendQueryParameter(ExternalKeys.EXPLORE_TAG, get(KEY_EXPLORE_TAG));
        return builder;
    }


    public static class Builder {
        private long mInitialTrackId;
        private String mOriginUrl;
        private String mExploreTag;
        private String mRecommenderVersion;

        public Builder(long initialTrackId){
            mInitialTrackId = initialTrackId;
        }

        public Builder originUrl(String originUrl){
            mOriginUrl = originUrl;
            return this;
        }

        public Builder exploreTag(String exploreTag){
            mExploreTag = exploreTag;
            return this;
        }

        public Builder recommenderVersion(String recommenderVersion){
            mRecommenderVersion = recommenderVersion;
            return this;
        }

        public PlaySourceInfo build(){
            return new PlaySourceInfo(this);
        }
    }
}
