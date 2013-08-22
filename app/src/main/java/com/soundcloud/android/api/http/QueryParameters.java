package com.soundcloud.android.api.http;

public enum QueryParameters {
    TOKENS("tokens");
    private String parameterKey;

    QueryParameters(String parameterKey) {
        this.parameterKey = parameterKey;
    }

    public String paramKey(){
        return parameterKey;
    }
}
