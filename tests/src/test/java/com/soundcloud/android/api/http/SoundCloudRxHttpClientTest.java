package com.soundcloud.android.api.http;

import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.AndroidCloudAPI;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.Before;
import org.mockito.Mock;

public class SoundCloudRxHttpClientTest {
    private SoundCloudRxHttpClient rxHttpClient;
    @Mock
    private AndroidCloudAPI androidCloudAPI;
    @Mock
    private HttpResponse httpResponse;
    @Mock
    private StatusLine statusLine;

    @Before
    public void setUp(){
        initMocks(this);
    }

}
