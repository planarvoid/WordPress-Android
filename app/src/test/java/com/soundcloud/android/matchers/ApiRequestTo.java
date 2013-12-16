package com.soundcloud.android.matchers;

import com.soundcloud.android.api.http.APIRequest;
import org.mockito.ArgumentMatcher;

class ApiRequestTo extends ArgumentMatcher<APIRequest> {

    private final String expectedPath;

    public ApiRequestTo(String expectedPath) {
        this.expectedPath = expectedPath;
    }

    @Override
    public boolean matches(Object argument) {
        return ((APIRequest) argument).getUriPath().equals(expectedPath);
    }
}
