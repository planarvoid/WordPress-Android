package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.api.ApiResponse;

public final class TestApiResponses {

    public static ApiResponse ok() {
        return new ApiResponse(null, 200, null);
    }

    public static ApiResponse status(int statusCode) {
        return new ApiResponse(null, statusCode, null);
    }

    private TestApiResponses() {
        // no instances
    }
}
