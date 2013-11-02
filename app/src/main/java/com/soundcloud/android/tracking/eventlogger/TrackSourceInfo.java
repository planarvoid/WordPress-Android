package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.tracking.eventlogger.PlayEventTracker.EventLoggerKeys;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.UriUtils;

import android.net.Uri;

public class TrackSourceInfo {

    public static final TrackSourceInfo EMPTY = new TrackSourceInfo();

    private static final String SOURCE_RECOMMENDER = "recommender";
    private static final String TRIGGER_AUTO = "auto";
    private static final String TRIGGER_MANUAL = "manual";

    private String mTrigger;
    private String mRecommenderVersion;

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

    @VisibleForTesting
    public String getTrigger(){
        return mTrigger;
    }

    @VisibleForTesting
    public String getRecommenderVersion() {
        return mRecommenderVersion;
    }

    public String createEventLoggerParams(PlaySourceInfo playSourceInfo){
        return createEventLoggerParams(playSourceInfo, null);
    }

    public String createEventLoggerParams(PlaySourceInfo playSourceInfo, Uri sourceUri){
        final Uri.Builder builder = new Uri.Builder();
        playSourceInfo.appendEventLoggerParams(builder);
        if (ScTextUtils.isNotBlank(mTrigger)){
            builder.appendQueryParameter(EventLoggerKeys.TRIGGER, mTrigger);
        }
        if (ScTextUtils.isNotBlank(mRecommenderVersion)){
            builder.appendQueryParameter(EventLoggerKeys.SOURCE, SOURCE_RECOMMENDER);
            builder.appendQueryParameter(EventLoggerKeys.SOURCE_VERSION, mRecommenderVersion);
        }
        if (sourceUri != null && Content.match(sourceUri) == Content.PLAYLIST) {
            builder.appendQueryParameter(EventLoggerKeys.SET, sourceUri.getLastPathSegment());
        }

        final String query = builder.build().getQuery();
        return ScTextUtils.isBlank(query) ? ScTextUtils.EMPTY_STRING : query.toString();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass()).add("trigger", getTrigger())
                .add("recommender_v", getRecommenderVersion()).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        TrackSourceInfo that = (TrackSourceInfo) o;
        return Objects.equal(mRecommenderVersion, that.mRecommenderVersion) && Objects.equal(mTrigger, that.mTrigger);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mTrigger, mRecommenderVersion);
    }
}
