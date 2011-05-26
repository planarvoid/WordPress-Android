package com.soundcloud.android.robolectric;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Env;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RoboApiBaseTests implements Endpoints {
    protected AndroidCloudAPI mockedApi;
    protected AndroidCloudAPI api;

    @Before
    public void setup() {
        mockedApi = mock(AndroidCloudAPI.class);
        api = new AndroidCloudAPI.Wrapper(null, null, null, new Token("1", "2"), Env.SANDBOX);
        when(mockedApi.getMapper()).thenReturn(AndroidCloudAPI.Wrapper.createMapper());
    }

    protected void expectGetRequestAndThrow(String request, Throwable e) throws IOException {
        when(mockedApi.get(requestMatcher(request))).thenThrow(e);
    }

    protected void expectGetRequestAndReturn(String request, int code, String resource) throws IOException {
        InputStream is = null;
        if (resource != null) {
            is = getClass().getResourceAsStream(resource);
            if (is == null) {
            is = new ByteArrayInputStream(resource.getBytes());
        }
        }
        HttpResponse resp = mock(HttpResponse.class);
        HttpEntity ent = mock(HttpEntity.class);
        StatusLine line = mock(StatusLine.class);

        when(ent.getContent()).thenReturn(is);
        when(resp.getEntity()).thenReturn(ent);
        when(line.getStatusCode()).thenReturn(code);
        when(resp.getStatusLine()).thenReturn(line);
        when(mockedApi.get(requestMatcher(request))).thenReturn(resp);
    }



    private Request requestMatcher(String request) {
        //return request == null ? any(Request.class) : RequestMatcher.isRequest(request);

        return any(Request.class);
    }

    static class RequestMatcher extends BaseMatcher<Request> {
        public RequestMatcher(String url) {
        }

        public static RequestMatcher isRequest(String url) {
            return new RequestMatcher(url);
        }

        @Override
        public boolean matches(Object o) {
            return false;
        }

        @Override
        public void describeTo(Description description) {
        }
    }

    protected String resource(String res) throws IOException {
        StringBuilder sb = new StringBuilder(65536);
        int n;
        byte[] buffer = new byte[8192];
        InputStream is = getClass().getResourceAsStream(res);
        while ((n = is.read(buffer)) != -1) sb.append(new String(buffer, 0, n));
        return sb.toString();
    }


}
