package com.soundcloud.android.matchers;

import com.soundcloud.api.Request;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.mockito.ArgumentMatcher;

public class SoundCloudMatchers {

    public static ApiRequestTo isApiRequestTo(String method, String path) {
        return new ApiRequestTo(method, path, false);
    }

    public static ApiRequestTo isPublicApiRequestTo(String method, String path) {
        return new ApiRequestTo(method, path, false);
    }

    public static ApiRequestTo isMobileApiRequestTo(String method, String path) {
        return new ApiRequestTo(method, path, true);
    }

    public static ArgumentMatcher<Request> isLegacyRequestToUrl(String url) {
        return new LegacyRequestTo(url);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Factory
    public static Matcher<String> urlEqualTo(String url) {
        return new UrlMatcher(url);
    }

    @Factory
    public static Matcher<String> queryStringEqualTo(String url) {
        return new AsQueryStringEqualsMatcher(url);
    }

}
