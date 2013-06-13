package com.soundcloud.android.api;

enum APIEndpoints {
    SUGGESTED_GENRE_AUDIO_CATEGORIES("/suggestions/users"),
    SUGGESTED_FACEBOOK_CATEGORIES("/suggestions/users/social/facebook");

    private String path;

    APIEndpoints(String path) {
        this.path = path;
    }

    public String path(){
        return path;
    }
}
