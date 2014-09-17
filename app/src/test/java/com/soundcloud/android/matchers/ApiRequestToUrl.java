package com.soundcloud.android.matchers;

import com.soundcloud.android.api.APIRequest;
import org.mockito.ArgumentMatcher;

import android.net.Uri;

public class ApiRequestToUrl extends ArgumentMatcher<APIRequest> {

    private final String expectedUri;
    private APIRequest request;

    public ApiRequestToUrl(String expectedUri) {
        this.expectedUri = expectedUri;
    }

    @Override
    public boolean matches(Object argument) {
        if (argument instanceof APIRequest) {
            this.request = (APIRequest) argument;
            return Uri.parse(expectedUri).equals(request.getUri());
        }
        return false;
    }

}
