package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.tracking.eventlogger.PlayEventTracker.EventLoggerKeys;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.soundcloud.android.utils.ScTextUtils;

import android.net.Uri;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class TrackSourceInfo {

    public static final TrackSourceInfo EMPTY = new TrackSourceInfo();

    private static final String SOURCE_RECOMMENDER = "recommender";
    private static final String SOURCE_EXPLORE = "explore";

    private static final String TRIGGER_AUTO = "auto";
    private static final String TRIGGER_MANUAL = "manual";

    private String mTrigger;
    private String mSource;
    private String mSourceVersion;

    public static TrackSourceInfo fromRecommender(String recommenderVersion){
        return fromSource(SOURCE_RECOMMENDER, recommenderVersion);
    }

    public static TrackSourceInfo fromExplore(String exploreVersion){
        return fromSource(SOURCE_EXPLORE, exploreVersion);
    }

    private static TrackSourceInfo fromSource(String source, String version){
        TrackSourceInfo trackSourceInfo = new TrackSourceInfo(source);
        trackSourceInfo.mSourceVersion = version;
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

    public String createEventLoggerParams(String originUrl){
        final String query = createEventLoggerBuilder(originUrl).build().getQuery();
        return ScTextUtils.isBlank(query) ? ScTextUtils.EMPTY_STRING : query.toString();
    }

    public String createEventLoggerParamsForSet(String setId, String setPosition, String originUrl){
        final Uri.Builder builder = createEventLoggerBuilder(originUrl);
        builder.appendQueryParameter(EventLoggerKeys.SET_ID, setId);
        builder.appendQueryParameter(EventLoggerKeys.SET_POSITION, setPosition);

        final String query = builder.build().getQuery();
        return ScTextUtils.isBlank(query) ? ScTextUtils.EMPTY_STRING : query.toString();
    }

    private Uri.Builder createEventLoggerBuilder(String originUrl) {
        final Uri.Builder builder = new Uri.Builder();
        if (ScTextUtils.isNotBlank(originUrl)){
            builder.appendQueryParameter(EventLoggerKeys.ORIGIN_URL, formatOriginUrl(originUrl));
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
        return builder;
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

    private String formatOriginUrl(String originUrl) {
        try {
            return URLEncoder.encode(originUrl.toLowerCase().replace(" ", "_"), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ScTextUtils.EMPTY_STRING;
    }
}
