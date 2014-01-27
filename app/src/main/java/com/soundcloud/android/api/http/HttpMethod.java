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
    },
    POST() {
        public HttpResponse execute(ApiWrapper wrapper, Request request) throws IOException {
            return wrapper.post(request);
        }
    },

    PUT() {
        public HttpResponse execute(ApiWrapper wrapper, Request request) throws IOException {
            return wrapper.put(request);
        }
    },

    DELETE() {
        public HttpResponse execute(ApiWrapper wrapper, Request request) throws IOException {
            return wrapper.delete(request);
        }
    };


    public abstract HttpResponse execute(ApiWrapper wrapper, Request request) throws IOException;
}