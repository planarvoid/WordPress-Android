package com.soundcloud.android.matchers;

import com.soundcloud.android.api.http.APIRequest;
import org.mockito.ArgumentMatcher;

public class SoundCloudMatchers {

    public static ArgumentMatcher<APIRequest> isApiRequestTo(String path) {
        return new ApiRequestTo(path);
    }

}
