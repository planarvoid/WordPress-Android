package com.soundcloud.android.api.legacy;

import com.google.common.base.Objects;

import org.apache.http.StatusLine;

import java.io.IOException;

public class UnexpectedResponseException extends IOException {
    private final Request request;
    private final StatusLine statusLine;

    public UnexpectedResponseException(Request request, StatusLine statusLine) {
        this.request = request;
        this.statusLine = statusLine;
    }

    public int getStatusCode() {
        return statusLine.getStatusCode();
    }

    @Override
    public String getMessage() {
        return toString();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("response_status", statusLine).add("request", request).toString();
    }
}
