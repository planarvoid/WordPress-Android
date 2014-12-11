package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.DeviceHelper;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class StrickSSLHttpClientTest {

    private final String USER_AGENT = "agent";
    private final String FILE_PATH = "/filepath";
    private final String OAUTH_TOKEN = "OAuth some token";

    @Mock private DeviceHelper deviceHelper;
    @Mock private OAuth oAuth;

    private OkHttpClient okHttp = new OkHttpClient();
    private MockWebServer server = new MockWebServer();
    private StrictSSLHttpClient httpClient;

    @Before
    public void setUp() throws Exception {
        httpClient = new StrictSSLHttpClient(okHttp, deviceHelper, oAuth);
        when(deviceHelper.getUserAgent()).thenReturn(USER_AGENT);
        when(oAuth.getAuthorizationHeaderValue()).thenReturn(OAUTH_TOKEN);
        playMockResponse();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void downloadFileRequestUsesGivenUrl() throws Exception {
        httpClient.downloadFile(server.getUrl(FILE_PATH).toString());

        final RecordedRequest request = server.takeRequest();
        expect(request.getPath()).toMatch(FILE_PATH);
    }

    @Test
    public void downloadFileRequestIncludesDeviceHeader() throws Exception {
        httpClient.downloadFile(server.getUrl(FILE_PATH).toString());

        final RecordedRequest request = server.takeRequest();
        expect(request.getHeader("User-Agent")).toEqual(USER_AGENT);
    }

    @Test
    public void downloadFileRequestIncludesOAuthToken() throws Exception {
        httpClient.downloadFile(server.getUrl(FILE_PATH).toString());

        final RecordedRequest request = server.takeRequest();
        expect(request.getHeader("Authorization")).toEqual(OAUTH_TOKEN);
    }

    private void playMockResponse() throws IOException {
        server.enqueue(new MockResponse());
        server.play();
    }

}
