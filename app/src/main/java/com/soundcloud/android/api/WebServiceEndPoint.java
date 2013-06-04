package com.soundcloud.android.api;

enum WebServiceEndPoint {
    SUGGESTED_CATEGORIES("/someUriTBD");

    private String path;

    WebServiceEndPoint(String path) {
        this.path = path;
    }

    public String path(){
        return path;
    }
}
