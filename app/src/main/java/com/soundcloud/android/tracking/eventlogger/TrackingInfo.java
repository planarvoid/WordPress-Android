package com.soundcloud.android.tracking.eventlogger;

import android.net.Uri;

import java.io.Serializable;

public class TrackingInfo implements Serializable {

    private static String KEY_SOURCE_CONTEXT = "tracking-sourceContext";
    private static String KEY_EXPLORE_TAG = "tracking-exploreTag";

    private String sourceContext;
    private String exploreTag;

    public TrackingInfo(String sourceContext) {
        this(sourceContext, null);
    }

    public TrackingInfo(String sourceContext, String exploreTag) {
        this.sourceContext = sourceContext;
        this.exploreTag = exploreTag;
    }

    /**
     * This exist purely for the PlayQueueUri to persist and recreate tracking through app lifecycles
     * {@link com.soundcloud.android.service.playback.PlayQueueUri}
     */
    public static TrackingInfo fromUriParams(Uri uri) {
        return new TrackingInfo(uri.getQueryParameter(KEY_SOURCE_CONTEXT), uri.getQueryParameter(KEY_EXPLORE_TAG));
    }

    public String getSourceContext() {
        return sourceContext;
    }

    public String getExploreTag() {
        return exploreTag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrackingInfo that = (TrackingInfo) o;

        if (exploreTag != null ? !exploreTag.equals(that.exploreTag) : that.exploreTag != null) return false;
        if (sourceContext != null ? !sourceContext.equals(that.sourceContext) : that.sourceContext != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = sourceContext != null ? sourceContext.hashCode() : 0;
        result = 31 * result + (exploreTag != null ? exploreTag.hashCode() : 0);
        return result;
    }

    public Uri.Builder appendAsQueryParams(Uri.Builder builder) {
        return builder.appendQueryParameter(KEY_SOURCE_CONTEXT, sourceContext)
                .appendQueryParameter(KEY_EXPLORE_TAG, exploreTag);
    }
}
