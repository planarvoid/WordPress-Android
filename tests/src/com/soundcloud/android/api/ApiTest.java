package com.soundcloud.android.api;

import android.util.Log;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.utils.ApiWrapper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.internal.Implements;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore
public abstract class ApiTest implements CloudAPI.Enddpoints {
    public CloudAPI api;
    public ObjectMapper mapper;

    @Before
    public void setUp() throws Exception {
        Robolectric.bindShadowClass(ShadowLog.class);

        api = mock(CloudAPI.class);
        when(api.getMapper()).thenReturn(getMapper());
    }

    public void fakeApi(String request, Throwable e) throws IOException {
        when(api.getContent(request)).thenThrow(e);
    }
    public void fakeApi(String request, String resource) throws IOException {
        InputStream is = getClass().getResourceAsStream(resource);
        fakeApi(request, is == null ? new ByteArrayInputStream(resource.getBytes()) : is);
    }

    public void fakeApi(String request, InputStream data) throws IOException {
        HttpResponse resp = mock(HttpResponse.class);
        HttpEntity ent = mock(HttpEntity.class);
        StatusLine line = mock(StatusLine.class);

        when(ent.getContent()).thenReturn(data);
        when(resp.getEntity()).thenReturn(ent);
        when(line.getStatusCode()).thenReturn(200);
        when(resp.getStatusLine()).thenReturn(line);

        when(api.getContent(request)).thenReturn(resp);
    }

    public ObjectMapper getMapper() {
        if (mapper == null) {
            mapper = new ApiWrapper().getMapper();
        }
        return mapper;
    }


    @SuppressWarnings({"UseOfSystemOutOrSystemErr", "UnusedDeclaration", "CallToPrintStackTrace"})
    @Implements(Log.class)
    public static class ShadowLog {
        public static int v(java.lang.String tag, java.lang.String msg) {
            System.out.println("[" + tag + "] " + msg);
            return 0;
        }

        public static int d(java.lang.String tag, java.lang.String msg) {
            System.out.println("[" + tag + "] " + msg);
            return 0;
        }

        public static int i(java.lang.String tag, java.lang.String msg) {
            System.out.println("[" + tag + "] " + msg);
            return 0;
        }

        public static int e(java.lang.String tag, java.lang.String msg, Throwable e) {
            System.out.println("[" + tag + "] " + msg);
            e.printStackTrace();
            return 0;
        }

        public static int w(java.lang.String tag, java.lang.String msg, Throwable e) {
            System.out.println("[" + tag + "] " + msg);
            e.printStackTrace();
            return 0;
        }
    }
}
