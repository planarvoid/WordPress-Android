package com.soundcloud.android.robolectric;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Params;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.Before;
import org.mockito.Matchers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RoboApiBaseTests implements Endpoints {
    protected AndroidCloudAPI api;

    @Before
    public void setup() {
        api = mock(AndroidCloudAPI.class);
        when(api.getMapper()).thenReturn(AndroidCloudAPI.Wrapper.createMapper());
    }

    protected void expectGetRequestAndThrow(String request, Throwable e) throws IOException {
        when(api.getContent(requestMatcher(request))).thenThrow(e);
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
        when(api.getContent(requestMatcher(request))).thenReturn(resp);
    }

    protected void expectPostRequestAndReturn(String request, int code, String resource) throws IOException {
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
        when(api.postContent(requestMatcher(request), Matchers.<Params>anyObject())).thenReturn(resp);
    }

    private String requestMatcher(String request) {
        return request == null ? anyString() : eq(request);
    }
}
