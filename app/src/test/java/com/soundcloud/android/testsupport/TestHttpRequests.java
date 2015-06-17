package com.soundcloud.android.testsupport;

import com.squareup.okhttp.Request;

public final class TestHttpRequests {

    public static Request.Builder get(String url) {
        return new Request.Builder().url(url).get();
    }

    public static Request stub() {
        return new Request.Builder().url("http://url").get().build();
    }

}
