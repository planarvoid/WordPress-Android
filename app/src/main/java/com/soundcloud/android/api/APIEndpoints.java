package com.soundcloud.android.api;

/**
 * TODO make package visible
 */
public enum APIEndpoints {
    SUGGESTED_USER_CATEGORIES("/app/mobileapps/suggestions/users/categories"),
    SUGGESTED_USER_FACEBOOK_CATEGORIES("/app/mobileapps/suggestions/users/social/facebook"),
    BULK_FOLLOW_USERS("/me/suggested/users");

    private String path;

    APIEndpoints(String path) {
        this.path = path;
    }

    public String path(){
        return path;
    }
}
