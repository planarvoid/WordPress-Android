package com.soundcloud.android.api;

/**
 * TODO make package visible
 */
public enum APIEndpoints {
    SUGGESTED_USER_CATEGORIES("/suggestions/users/categories"),
    SUGGESTED_USER_FACEBOOK_CATEGORIES("/suggestions/users/social/facebook"),
    BULK_FOLLOW_USERS("/me/suggested/users"),
    EXPLORE_TRACKS_CATEGORIES("/suggestions/tracks/categories");

    private String path;

    APIEndpoints(String path) {
        this.path = path;
    }

    public String path(){
        return path;
    }
}
