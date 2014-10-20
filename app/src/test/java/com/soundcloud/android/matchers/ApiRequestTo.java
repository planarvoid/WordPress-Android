package com.soundcloud.android.matchers;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.soundcloud.android.api.ApiRequest;
import org.hamcrest.Description;
import org.mockito.ArgumentMatcher;

import java.util.HashMap;
import java.util.Map;

public class ApiRequestTo extends ArgumentMatcher<ApiRequest> {

    private final String expectedMethod, expectedPath;
    private final boolean isMobileApi;
    private Map<String, String> expectedQueryParams = new HashMap<String, String>();
    private boolean queryMatchError;
    private ApiRequest request;
    private Object content;

    public ApiRequestTo(String expectedMethod, String expectedPath, boolean isMobileApi) {
        this.expectedMethod = expectedMethod;
        this.expectedPath = expectedPath;
        this.isMobileApi = isMobileApi;
    }

    @Override
    public boolean matches(Object argument) {
        if (argument instanceof ApiRequest) {
            this.request = (ApiRequest) argument;

            boolean queryMatches;
            for (Map.Entry<String, String> param : expectedQueryParams.entrySet()) {
                queryMatches = request.getQueryParameters().containsEntry(param.getKey(), param.getValue());
                if (!queryMatches) {
                    queryMatchError = true;
                    return false;
                }
            }

            return contentMatches() &&
                    request.getEncodedPath().equals(expectedPath) &&
                    request.getMethod().equalsIgnoreCase(expectedMethod) &&
                    request.isPrivate() == isMobileApi;
        }
        return false;
    }

    private boolean contentMatches() {
        if (request.getContent() == null) {
            return content == null;

        } else if (content == null) {
            return false;

        } else if (content instanceof Iterable) {
            return request.getContent() instanceof Iterable
                    && Iterables.elementsEqual((Iterable) content, (Iterable) request.getContent());
        } else {
            return Objects.equal(request.getContent(), content);
        }
    }

    public ApiRequestTo withQueryParam(String key, String value) {
        expectedQueryParams.put(key, value);
        return this;
    }

    public ApiRequestTo withContent(Object content){
        this.content = content;
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
