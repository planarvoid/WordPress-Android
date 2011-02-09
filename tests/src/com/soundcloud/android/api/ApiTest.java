package com.soundcloud.android.api;

import com.soundcloud.android.CloudAPI;
import org.junit.Before;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ApiTest {
    public CloudAPI api;

    @Before
    public void setUp() throws Exception {
        api = mock(CloudAPI.class);
    }

    public void fakeApi(String request, String resource) throws IOException {
        InputStream is = getClass().getResourceAsStream(resource);
        fakeApi(request, is == null ? new ByteArrayInputStream(resource.getBytes()) : is);
    }

    public void fakeApi(String request, InputStream data) throws IOException {
        when(api.executeRequest(request)).thenReturn(data);
    }

}
