package com.soundcloud.api;

import org.apache.http.HttpResponse;

import java.io.IOException;

/**
 * Thrown if the service API responds in error. The HTTP status code can be obtained via {@link #getStatusCode()}.
 */
public class ApiResponseException extends IOException {
    private static final long serialVersionUID = -2990651725862868387L;

    public final HttpResponse response;

    public ApiResponseException(HttpResponse resp, String error) {
        super(resp.getStatusLine().getStatusCode() + ": [" + resp.getStatusLine().getReasonPhrase() + "] "
                + (error != null ? error : ""));
        this.response = resp;
    }

    public ApiResponseException(Throwable throwable, HttpResponse response) {
        super(throwable == null ? null : throwable.toString());
        initCause(throwable);
        this.response = response;
    }

    public int getStatusCode() {
        return response.getStatusLine().getStatusCode();
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " " + (response != null ? response.getStatusLine() : "");
    }
}
