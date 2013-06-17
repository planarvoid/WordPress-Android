package com.soundcloud.android.api;

enum APIEndpoints {
    SUGGESTED_USER_CATEGORIES("/app/mobileapps/suggestions/users/categories"),
    SUGGESTED_USER_FACEBOOK_CATEGORIES("/app/mobileapps/suggestions/users/social/facebook");

    private String path;

    APIEndpoints(String path) {
        this.path = path;
    }

    public String path(){
        return path;
    }
}
