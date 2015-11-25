package com.soundcloud.android.testsupport.matchers;

import com.soundcloud.android.api.ApiRequest;
import org.mockito.ArgumentMatcher;

import android.net.Uri;

public class ApiRequestToUrl extends ArgumentMatcher<ApiRequest> {

    private final String expectedUri;

    public ApiRequestToUrl(String expectedUri) {
        this.expectedUri = expectedUri;
    }

    @Override
    public boolean matches(Object argument) {
        if (argument instanceof ApiRequest) {
            ApiRequest request = (ApiRequest) argument;
            return Uri.parse(expectedUri).equals(request.getUri());
        }
        return false;
    }

}
