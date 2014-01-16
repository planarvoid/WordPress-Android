package com.soundcloud.android.matchers;

import com.soundcloud.android.api.http.APIRequest;
import org.mockito.ArgumentMatcher;

class ApiRequestTo extends ArgumentMatcher<APIRequest> {

    private final String expectedMethod, expectedPath;
    private final boolean isMobileApi;

    public ApiRequestTo(String expectedMethod, String expectedPath, boolean isMobileApi) {
        this.expectedMethod = expectedMethod;
        this.expectedPath = expectedPath;
        this.isMobileApi = isMobileApi;
    }

    @Override
    public boolean matches(Object argument) {
        if (argument instanceof APIRequest) {
            APIRequest request = (APIRequest) argument;
            return request.getUriPath().equals(expectedPath) &&
                    request.getMethod().equalsIgnoreCase(expectedMethod) &&
                    request.isPrivate() == isMobileApi;
        }
        return false;
    }
}
