package com.soundcloud.android.tracking.eventlogger;

import android.net.Uri;

public class PlaySourceInfo extends ParamsMap {

    private static String KEY_ORIGIN_URL = "playSource-originUrl";
    private static String KEY_EXPLORE_TAG = "playSource-exploreTag";

    public PlaySourceInfo(String originUrl) {
        super(1);
        put(KEY_ORIGIN_URL, originUrl);
    }

    public PlaySourceInfo(String originUrl, String exploreTag) {
        super(2);
        put(KEY_ORIGIN_URL, originUrl);
        put(KEY_EXPLORE_TAG, exploreTag);
    }

    /**
     * This exist purely for the PlayQueueUri to persist and recreate tracking through app lifecycles
     * {@link com.soundcloud.android.service.playback.PlayQueueUri}
     */
    public static PlaySourceInfo fromUriParams(Uri uri) {
        return new PlaySourceInfo(uri.getQueryParameter(KEY_ORIGIN_URL), uri.getQueryParameter(KEY_EXPLORE_TAG));
    }
}
