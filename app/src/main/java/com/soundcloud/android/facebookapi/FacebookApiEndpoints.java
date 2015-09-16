package com.soundcloud.android.facebookapi;


public enum FacebookApiEndpoints {
    ME_FRIEND_PICTURES("/me/friends", "picture.type(normal)");

    private final String path;
    private final String fields;

    FacebookApiEndpoints(String path, String fields) {
        this.path = path;
        this.fields = fields;
    }

    public String getPath() {
        return path;
    }

    public String getFields() {
        return fields;
    }
}
