package com.soundcloud.android.matchers;

import com.soundcloud.api.Request;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.mockito.ArgumentMatcher;

public class SoundCloudMatchers {

    public static ApiRequestTo isApiRequestTo(String method, String path) {
        return new ApiRequestTo(method, path, true);
    }

    public static ApiRequestTo isApiRequestMethod(String method) {
        return new ApiRequestTo(method, true);
    }

    public static ApiRequestTo isPublicApiRequestMethod(String method) {
        return new ApiRequestTo(method, false);
    }

    public static ApiRequestToUrl isApiRequestTo(String url) {
        return new ApiRequestToUrl(url);
    }

    public static ApiRequestTo isPublicApiRequestTo(String method, String path) {
        return new ApiRequestTo(method, path, false);
    }

    @Deprecated // use isApiRequestTo
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
