package com.soundcloud.android.api;


import com.google.common.annotations.VisibleForTesting;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;

import java.util.Collection;

public class APIResponse {
    private final StatusLine statusLine;
    private final HttpResponse httpResponse;

    protected APIResponse(HttpResponse httpResponse) {
        this.httpResponse = httpResponse;
        this.statusLine = httpResponse.getStatusLine();
    }

    public boolean isSuccess() {
        //TODO Wondering if this should not go beyond 300 status code
        return statusLine.getStatusCode() >= HttpStatus.SC_OK && statusLine.getStatusCode() < HttpStatus.SC_BAD_REQUEST;
    }

    public boolean isNotSuccess(){
        return !isSuccess();
    }

    public <T> Collection<T> getCollection() {
        return null;
    }

    @VisibleForTesting
    protected HttpResponse getWrappedResponse() {
        return httpResponse;
    }
}
