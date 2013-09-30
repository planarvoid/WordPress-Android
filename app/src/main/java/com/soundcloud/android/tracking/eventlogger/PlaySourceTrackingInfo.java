package com.soundcloud.android.tracking.eventlogger;

import android.net.Uri;

import java.io.Serializable;

public class PlaySourceTrackingInfo implements Serializable {

    private static String KEY_ORIGIN_URL = "tracking-originUrl";
    private static String KEY_EXPLORE_TAG = "tracking-exploreTag";

    private String originUrl;
    private String exploreTag;


    public PlaySourceTrackingInfo(String originUrl) {
        this(originUrl, null);
    }

    public PlaySourceTrackingInfo(String originUrl, String exploreTag) {
        this.originUrl = originUrl;
        this.exploreTag = exploreTag;
    }

    /**
     * This exist purely for the PlayQueueUri to persist and recreate tracking through app lifecycles
     * {@link com.soundcloud.android.service.playback.PlayQueueUri}
     */
    public static PlaySourceTrackingInfo fromUriParams(Uri uri) {
        return new PlaySourceTrackingInfo(uri.getQueryParameter(KEY_ORIGIN_URL), uri.getQueryParameter(KEY_EXPLORE_TAG));
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

        PlaySourceTrackingInfo that = (PlaySourceTrackingInfo) o;

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

    public String toQueryParams() {
        return appendAsQueryParams(new Uri.Builder()).build().getQuery().toString();
    }

    public Uri.Builder appendAsQueryParams(Uri.Builder builder) {
        return builder.appendQueryParameter(KEY_ORIGIN_URL, originUrl)
                .appendQueryParameter(KEY_EXPLORE_TAG, exploreTag);
    }
}
