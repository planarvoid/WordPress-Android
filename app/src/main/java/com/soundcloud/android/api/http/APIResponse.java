package com.soundcloud.android.api.http;


import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

class APIResponse {
    private static final int SC_REQUEST_TOO_MANY_REQUESTS = 429;

    private final int mStatusCode;
    private final String mResponseBody;
    private final Map<String, String> mResponseHeaders;

    protected APIResponse(int statusCode, String responseBody, Map<String, String> responseHeaders) throws IOException {
        mStatusCode = statusCode;
        mResponseBody = responseBody;
        mResponseHeaders = responseHeaders;
    }

    public boolean isSuccess() {
        //TODO Wondering if this should not go beyond 300 status code
        return mStatusCode >= HttpStatus.SC_OK && mStatusCode < HttpStatus.SC_BAD_REQUEST;
    }

    public boolean accountIsRateLimited(){
        return mStatusCode == SC_REQUEST_TOO_MANY_REQUESTS;
    }

    public String getHeader(String key){
        return mResponseHeaders.get(key);
    }

    public boolean responseContainsHeader(String key){
        return mResponseHeaders.containsKey(key);
    }

    public String getResponseBody(){
        return mResponseBody;
    }

    public boolean isNotSuccess(){
        return !isSuccess();
    }

    public <T> Collection<T> getCollection() {
        return null;
    }

}
