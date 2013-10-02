package com.soundcloud.android.tracking.eventlogger;

import com.google.common.base.Objects;
import com.soundcloud.android.utils.ScTextUtils;

import android.net.Uri;

public class TrackSourceInfo implements EventLoggerParams {

    public static final TrackSourceInfo EMPTY = new TrackSourceInfo();

    private static final String SOURCE_RECOMMENDER = "recommender";
    private static final String TRIGGER_AUTO = "auto";
    private static final String TRIGGER_MANUAL = "manual";

    private String mTrigger;
    private String mRecommenderVersion;


    @Override
    public Uri.Builder appendEventLoggerParams(Uri.Builder builder) {
        builder.appendQueryParameter(ExternalKeys.TRIGGER, mTrigger);
        if (ScTextUtils.isNotBlank(mRecommenderVersion)){
            builder.appendQueryParameter(ExternalKeys.SOURCE, SOURCE_RECOMMENDER);
            builder.appendQueryParameter(ExternalKeys.SOURCE_VERSION, mRecommenderVersion);
        }
        return builder;
    }

    public static TrackSourceInfo fromRecommender(String recommenderVersion){
        TrackSourceInfo trackSourceInfo = new TrackSourceInfo();
        trackSourceInfo.mRecommenderVersion = recommenderVersion;
        trackSourceInfo.mTrigger = TRIGGER_AUTO;
        return trackSourceInfo;
    }

    public static TrackSourceInfo auto(){
        TrackSourceInfo trackSourceInfo = new TrackSourceInfo();
        trackSourceInfo.mTrigger = TRIGGER_AUTO;
        return trackSourceInfo;
    }

    public static TrackSourceInfo manual(){
        TrackSourceInfo trackSourceInfo = new TrackSourceInfo();
        trackSourceInfo.mTrigger = TRIGGER_MANUAL;
        return trackSourceInfo;
    }

    public String getTrigger(){
        return mTrigger;
    }

    public String getRecommenderVersion() {
        return mRecommenderVersion;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass()).add("trigger", getTrigger())
                .add("recommender_v", getRecommenderVersion()).toString();
    }
}
