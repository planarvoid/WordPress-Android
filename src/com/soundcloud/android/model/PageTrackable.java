package com.soundcloud.android.model;

public interface PageTrackable {
    /**
     * Generates page tracking information for GA
     *
     * @param parameters optional parameters (model specific)
     * @return the tracking path
     */
    String pageTrack(String... parameters);
}
