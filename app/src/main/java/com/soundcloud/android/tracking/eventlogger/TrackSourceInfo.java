package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.tracking.eventlogger.PlayEventTracker.EventLoggerKeys;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;

import android.net.Uri;

public class TrackSourceInfo {

    public static final TrackSourceInfo EMPTY = new TrackSourceInfo();

    private static final String SOURCE_RECOMMENDER = "recommender";
    private static final String SOURCE_EXPLORE = "explore";

    private static final String TRIGGER_AUTO = "auto";
    private static final String TRIGGER_MANUAL = "manual";

    private String mTrigger;
    private String mSource;
    private String mSourceVersion;
    private String mOriginUrl;

    public static TrackSourceInfo fromRecommender(String recommenderVersion, String originUrl){
        return fromSource(SOURCE_RECOMMENDER, recommenderVersion, originUrl);
    }

    public static TrackSourceInfo fromExplore(String exploreVersion, String originUrl){
        return fromSource(SOURCE_EXPLORE, exploreVersion, originUrl);
    }

    private static TrackSourceInfo fromSource(String source, String version, String originUrl){
        TrackSourceInfo trackSourceInfo = new TrackSourceInfo(source);
        trackSourceInfo.mSourceVersion = version;
        trackSourceInfo.mOriginUrl = originUrl;
        return trackSourceInfo;
    }

    public TrackSourceInfo() { }

    public TrackSourceInfo(String source) {
        this.mSource = source;
    }

    public TrackSourceInfo setTrigger(boolean manualTrigger){
        mTrigger = manualTrigger ? TRIGGER_MANUAL : TRIGGER_AUTO;
        return this;
    }

    @VisibleForTesting
    public String getTrigger(){
        return mTrigger;
    }

    @VisibleForTesting
    public String getSourceVersion() {
        return mSourceVersion;
    }

    public String getSource() {
        return mSource;
    }

    public String createEventLoggerParams(Uri sourceUri){

        final Uri.Builder builder = new Uri.Builder();
        if (ScTextUtils.isNotBlank(mOriginUrl)){
            builder.appendQueryParameter(EventLoggerKeys.ORIGIN_URL, mOriginUrl);
        }

        if (ScTextUtils.isNotBlank(mTrigger)){
            builder.appendQueryParameter(EventLoggerKeys.TRIGGER, mTrigger);
        }

        if (ScTextUtils.isNotBlank(mSource)){
            builder.appendQueryParameter(EventLoggerKeys.SOURCE, mSource);
        }

        if (ScTextUtils.isNotBlank(mSourceVersion)){
            builder.appendQueryParameter(EventLoggerKeys.SOURCE_VERSION, mSourceVersion);
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
                .add("source", getSource()).add("source_v", getSourceVersion()).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        TrackSourceInfo that = (TrackSourceInfo) o;
        return Objects.equal(mSource, that.mSource)
                && Objects.equal(mSourceVersion, that.mSourceVersion)
                && Objects.equal(mTrigger, that.mTrigger);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mTrigger, mSource, mSourceVersion);
    }
}
