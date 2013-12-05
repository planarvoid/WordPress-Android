package com.soundcloud.android.api.http;

import javax.inject.Inject;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpURLConnectionFactory {

    @Inject
    public HttpURLConnectionFactory() {
    }

    public HttpURLConnection create(String url) throws IOException {
        return (HttpURLConnection) new URL(url).openConnection();
    }
}
