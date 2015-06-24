package com.soundcloud.android.testsupport.matchers;

import com.soundcloud.api.Request;
import org.mockito.ArgumentMatcher;

class LegacyRequestTo extends ArgumentMatcher<Request> {

    private final String expectedUrl;

    public LegacyRequestTo(String expectedUrl) {
        this.expectedUrl = expectedUrl;
    }

    @Override
    public boolean matches(Object argument) {
        if (argument instanceof Request) {
            Request request = (Request) argument;
            return request.toUrl().equals(expectedUrl);
        }
        return false;
    }
}
