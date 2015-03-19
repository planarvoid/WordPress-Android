package com.soundcloud.android.api;

import java.io.IOException;

public final class TestApiResponses {

    public static ApiResponse ok() {
        return new ApiResponse(null, 200, null);
    }

    public static ApiResponse status(int statusCode) {
        return new ApiResponse(null, statusCode, null);
    }

    public static ApiResponse networkError() {
        return new ApiResponse(ApiRequestException.networkError(null, new IOException()));
    }

    private TestApiResponses() {
        // no instances
    }
}
