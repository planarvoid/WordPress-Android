package com.soundcloud.android.matchers;

import com.soundcloud.android.api.http.APIRequest;
import org.hamcrest.Description;
import org.mockito.ArgumentMatcher;

import java.util.HashMap;
import java.util.Map;

public class ApiRequestTo extends ArgumentMatcher<APIRequest> {

    private final String expectedMethod, expectedPath;
    private final boolean isMobileApi;
    private Map<String, String> expectedQueryParams = new HashMap<String, String>();
    private boolean queryMatchError;
    private APIRequest request;

    public ApiRequestTo(String expectedMethod, String expectedPath, boolean isMobileApi) {
        this.expectedMethod = expectedMethod;
        this.expectedPath = expectedPath;
        this.isMobileApi = isMobileApi;
    }

    @Override
    public boolean matches(Object argument) {
        if (argument instanceof APIRequest) {
            this.request = (APIRequest) argument;

            boolean queryMatches = true;
            for (Map.Entry<String, String> param : expectedQueryParams.entrySet()) {
                queryMatches = request.getQueryParameters().containsEntry(param.getKey(), param.getValue());
                if (!queryMatches) {
                    queryMatchError = true;
                    return false;
                }
            }

            return request.getEncodedPath().equals(expectedPath) &&
                    request.getMethod().equalsIgnoreCase(expectedMethod) &&
                    request.isPrivate() == isMobileApi;
        }
        return false;
    }

    public ApiRequestTo withQueryParam(String key, String value) {
        expectedQueryParams.put(key, value);
        return this;
    }

    @Override
    public void describeTo(Description description) {
        if (queryMatchError) {
            description.appendText("Expected query params to contain ");
            description.appendValue(expectedQueryParams);
            description.appendText("\nBut found ");
            description.appendValue(request.getQueryParameters());
        } else {
            super.describeTo(description);
        }
    }
}
