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

    public static ApiResponse validationError() {
        return new ApiResponse(ApiRequestException.validationError(null, "validation_failed"));
    }

    private TestApiResponses() {
        // no instances
    }
}
