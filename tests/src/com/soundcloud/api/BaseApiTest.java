package com.soundcloud.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Ignore
public abstract class BaseApiTest implements CloudAPI.Endpoints {
    public CloudAPI api;
    public ObjectMapper mapper;

    @Before
    public void setUp() throws Exception {
        api = mock(CloudAPI.class);
    }

    protected void expectGetRequestAndThrow(String request, Throwable e) throws IOException {
        when(api.getContent(request)).thenThrow(e);
    }

    protected void expectGetRequestAndReturn(String request, String resource) throws IOException {
        InputStream is = getClass().getResourceAsStream(resource);
        expectGetRequestAndReturn(request, is == null ? new ByteArrayInputStream(resource.getBytes()) : is);
    }

    protected void expectGetRequestAndReturn(String request, InputStream data) throws IOException {
        HttpResponse resp = mock(HttpResponse.class);
        HttpEntity ent = mock(HttpEntity.class);
        StatusLine line = mock(StatusLine.class);

        when(ent.getContent()).thenReturn(data);
        when(resp.getEntity()).thenReturn(ent);
        when(line.getStatusCode()).thenReturn(200);
        when(resp.getStatusLine()).thenReturn(line);
        when(api.getContent(request)).thenReturn(resp);
    }
}
