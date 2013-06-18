package com.soundcloud.android.api.http;

import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;

import java.io.IOException;

enum HttpMethod {
    GET() {
        public HttpResponse execute(ApiWrapper wrapper, Request request) throws IOException {
            return wrapper.get(request);
        }
    };


    public abstract HttpResponse execute(ApiWrapper wrapper, Request request) throws IOException;
}