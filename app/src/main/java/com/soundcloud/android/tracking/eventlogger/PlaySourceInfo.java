package com.soundcloud.android.tracking.eventlogger;

import com.google.common.primitives.Longs;

import android.net.Uri;

public class PlaySourceInfo extends ParamsMap {

    private static final String KEY_ORIGIN_URL = "playSource-originUrl";
    private static final String KEY_EXPLORE_TAG = "playSource-exploreTag";
    private static final String KEY_INITIAL_TRACK_ID = "playSource-initialTrackId";
    private static final String KEY_RECOMMENDER_VERSION = "playSource-recommenderVersion";

    public PlaySourceInfo(String originUrl, long initialTrackId) {
        super(4); // expect capacity set to key count. Up this if we add more keys
        put(KEY_ORIGIN_URL, originUrl);
        put(KEY_INITIAL_TRACK_ID, String.valueOf(initialTrackId));
    }

    public PlaySourceInfo(String originUrl, long initialTrackId, String exploreTag) {
        this(originUrl, initialTrackId);
        put(KEY_EXPLORE_TAG, exploreTag);
    }

    public PlaySourceInfo(String originUrl, long initialTrackId, String exploreTag, String recommenderVersion) {
        this(originUrl, initialTrackId, exploreTag);
        put(KEY_RECOMMENDER_VERSION, recommenderVersion);
    }

    /**
     * This exist purely for the PlayQueueUri to persist and recreate tracking through app lifecycles
     * {@link com.soundcloud.android.service.playback.PlayQueueUri}
     */
    public static PlaySourceInfo fromUriParams(Uri uri) {
        return new PlaySourceInfo(
                uri.getQueryParameter(KEY_ORIGIN_URL),
                Longs.tryParse(uri.getQueryParameter(KEY_INITIAL_TRACK_ID)),
                uri.getQueryParameter(KEY_EXPLORE_TAG),
                uri.getQueryParameter(KEY_RECOMMENDER_VERSION)
        );
    }
}
