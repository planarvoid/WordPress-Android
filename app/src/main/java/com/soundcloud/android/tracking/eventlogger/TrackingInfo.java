package com.soundcloud.android.tracking.eventlogger;

import android.net.Uri;

import java.io.Serializable;

public class TrackingInfo implements Serializable {

    private static String KEY_ORIGIN_URL = "tracking-originUrl";
    private static String KEY_EXPLORE_TAG = "tracking-exploreTag";

    private String originUrl;
    private String exploreTag;

    public TrackingInfo(String originUrl) {
        this(originUrl, null);
    }

    public TrackingInfo(String originUrl, String exploreTag) {
        this.originUrl = originUrl;
        this.exploreTag = exploreTag;
    }

    /**
     * This exist purely for the PlayQueueUri to persist and recreate tracking through app lifecycles
     * {@link com.soundcloud.android.service.playback.PlayQueueUri}
     */
    public static TrackingInfo fromUriParams(Uri uri) {
        return new TrackingInfo(uri.getQueryParameter(KEY_ORIGIN_URL), uri.getQueryParameter(KEY_EXPLORE_TAG));
    }

    public String getOriginUrl() {
        return originUrl;
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
        if (originUrl != null ? !originUrl.equals(that.originUrl) : that.originUrl != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = originUrl != null ? originUrl.hashCode() : 0;
        result = 31 * result + (exploreTag != null ? exploreTag.hashCode() : 0);
        return result;
    }

    public Uri.Builder appendAsQueryParams(Uri.Builder builder) {
        return builder.appendQueryParameter(KEY_ORIGIN_URL, originUrl)
                .appendQueryParameter(KEY_EXPLORE_TAG, exploreTag);
    }
}
