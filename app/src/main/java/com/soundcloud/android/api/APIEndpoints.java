package com.soundcloud.android.api;

/**
 * TODO make package visible
 */
public enum APIEndpoints {
    // mobile API
    SUGGESTED_USER_CATEGORIES("/suggestions/users/categories"),
    SUGGESTED_USER_FACEBOOK_CATEGORIES("/suggestions/users/social/facebook"),
    EXPLORE_TRACKS_POPULAR_MUSIC("/suggestions/tracks/categories/popular+music"),
    EXPLORE_TRACKS_POPULAR_AUDIO("/suggestions/tracks/categories/popular+audio"),
    EXPLORE_TRACKS_CATEGORIES("/suggestions/tracks/categories"),
    RELATED_TRACKS("/tracks/%s/related"),

    // public API (DEPRECATED)
    CURRENT_USER("/me"),
    BULK_FOLLOW_USERS("/me/suggested/users");

    private String path;

    APIEndpoints(String path) {
        this.path = path;
    }

    public String path(){
        return path;
    }
}
