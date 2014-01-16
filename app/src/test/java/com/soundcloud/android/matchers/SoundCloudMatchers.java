package com.soundcloud.android.matchers;

import com.soundcloud.android.api.http.APIRequest;
import org.mockito.ArgumentMatcher;

public class SoundCloudMatchers {

    public static ArgumentMatcher<APIRequest> isApiRequestTo(String method, String path) {
        return new ApiRequestTo(method, path, false);
    }

    public static ArgumentMatcher<APIRequest> isPublicApiRequestTo(String method, String path) {
        return new ApiRequestTo(method, path, false);
    }

    public static ArgumentMatcher<APIRequest> isMobileApiRequestTo(String method, String path) {
        return new ApiRequestTo(method, path, true);
    }

}
