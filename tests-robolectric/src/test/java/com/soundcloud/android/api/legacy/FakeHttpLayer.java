package com.soundcloud.android.api.legacy;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FakeHttpLayer {
    List<HttpResponse> pendingHttpResponses = new ArrayList<>();
    List<HttpEntityStub.ResponseRule> httpResponseRules = new ArrayList<>();
    HttpResponse defaultHttpResponse;

    public void addPendingHttpResponse(int statusCode, String responseBody) {
        addPendingHttpResponse(new FakeHttpResponse(statusCode, responseBody));
    }

    public void addPendingHttpResponse(HttpResponse httpResponse) {
        pendingHttpResponses.add(httpResponse);
    }

    public void addHttpResponseRule(String method, String uri, HttpResponse response) {
        addHttpResponseRule(new DefaultRequestMatcher(method, uri), response);
    }

    public void addHttpResponseRule(String uri, HttpResponse response) {
        addHttpResponseRule(new UriRequestMatcher(uri), response);
    }

    public void addHttpResponseRule(String uri, String response) {
        addHttpResponseRule(new UriRequestMatcher(uri), new FakeHttpResponse(200, response));
    }

    public void addHttpResponseRule(RequestMatcher requestMatcher, HttpResponse response) {
        addHttpResponseRule(new RequestMatcherResponseRule(requestMatcher, response));
    }

    public void addHttpResponseRule(HttpEntityStub.ResponseRule responseRule) {
        httpResponseRules.add(responseRule);
    }

    private HttpResponse findResponse(HttpRequest httpRequest) throws HttpException, IOException {
        if (!pendingHttpResponses.isEmpty()) {
            return pendingHttpResponses.remove(0);
        }

        for (HttpEntityStub.ResponseRule httpResponseRule : httpResponseRules) {
            if (httpResponseRule.matches(httpRequest)) {
                return httpResponseRule.getResponse();
            }
        }

        return defaultHttpResponse;
    }

    public HttpResponse emulateRequest(HttpRequest httpRequest) throws HttpException, IOException {
        HttpResponse httpResponse = findResponse(httpRequest);

        if (httpResponse == null) {
            throw new RuntimeException("Unexpected call to execute, no pending responses are available. See Robolectric.addPendingResponse().");
        }

        return httpResponse;
    }

    public void clearHttpResponseRules() {
        httpResponseRules.clear();
    }

    public static class RequestMatcherResponseRule implements HttpEntityStub.ResponseRule {
        private RequestMatcher requestMatcher;
        private HttpResponse responseToGive;

        public RequestMatcherResponseRule(RequestMatcher requestMatcher, HttpResponse responseToGive) {
            this.requestMatcher = requestMatcher;
            this.responseToGive = responseToGive;
        }

        @Override
        public boolean matches(HttpRequest request) {
            return requestMatcher.matches(request);
        }

        @Override
        public HttpResponse getResponse() throws HttpException, IOException {
            return responseToGive;
        }
    }

    public static class DefaultRequestMatcher implements RequestMatcher {
        private String method;
        private String uri;

        public DefaultRequestMatcher(String method, String uri) {
            this.method = method;
            this.uri = uri;
        }

        @Override
        public boolean matches(HttpRequest request) {
            return request.getRequestLine().getMethod().equals(method) &&
                    request.getRequestLine().getUri().equals(uri);
        }
    }

    public static class UriRequestMatcher implements RequestMatcher {
        private String uri;

        public UriRequestMatcher(String uri) {
            this.uri = uri;
        }

        @Override
        public boolean matches(HttpRequest request) {
            return request.getRequestLine().getUri().equals(uri);
        }
    }
}
