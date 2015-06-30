package com.soundcloud.android.api.legacy;

import org.apache.http.HttpRequest;

public interface RequestMatcher {
    public boolean matches(HttpRequest request);
}
