package com.soundcloud.android.api;

import com.soundcloud.android.testsupport.fixtures.JsonFixtures;

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
        return new ApiResponse(ApiRequestException.validationError(null, "validation_failed", 101));
    }

    public static ApiResponse resource(Class clazz, int statusCode, String resourceName) throws IOException {
        return new ApiResponse(null, statusCode, JsonFixtures.resourceAsString(clazz, resourceName));
    }

    private TestApiResponses() {
        // no instances
    }
}
