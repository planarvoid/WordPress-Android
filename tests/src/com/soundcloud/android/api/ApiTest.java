package com.soundcloud.android.api;

import android.util.Log;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.internal.Implements;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ApiTest {
    public CloudAPI api;
    public ObjectMapper mapper;

    @Before
    public void setUp() throws Exception {
        Robolectric.bindShadowClass(ShadowLog.class);

        api = mock(CloudAPI.class);
        when(api.getMapper()).thenReturn(getMapper());
    }

    public void fakeApi(String request, Throwable e) throws IOException {
        when(api.executeRequest(request)).thenThrow(e);
    }
    public void fakeApi(String request, String resource) throws IOException {
        InputStream is = getClass().getResourceAsStream(resource);
        fakeApi(request, is == null ? new ByteArrayInputStream(resource.getBytes()) : is);
    }

    public void fakeApi(String request, InputStream data) throws IOException {
        when(api.executeRequest(request)).thenReturn(data);
    }

    public ObjectMapper getMapper() {
        if (mapper == null) {
            mapper = new SoundCloudApplication().getMapper();
        }
        return mapper;
    }


    @SuppressWarnings({"UseOfSystemOutOrSystemErr", "UnusedDeclaration", "CallToPrintStackTrace"})
    @Implements(Log.class)
    static class ShadowLog {
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
