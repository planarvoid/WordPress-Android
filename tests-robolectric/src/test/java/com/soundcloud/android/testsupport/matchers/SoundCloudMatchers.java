package com.soundcloud.android.testsupport.matchers;

import com.soundcloud.android.api.ApiEndpoints;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;

public class SoundCloudMatchers {

    public static ApiRequestTo isApiRequestTo(String method, String path) {
        return new ApiRequestTo(method, path, true);
    }

    public static ApiRequestToUrl isApiRequestTo(String url) {
        return new ApiRequestToUrl(url);
    }

    public static ApiRequestTo isPublicApiRequestTo(String method, String path) {
        return new ApiRequestTo(method, path, false);
    }

    public static ApiRequestTo isPublicApiRequestTo(String method, ApiEndpoints endpoint) {
        return new ApiRequestTo(method, endpoint.path(), false);
    }

    @Factory
    public static Matcher<String> urlEqualTo(String url) {
        return new UrlMatcher(url);
    }
}
