package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.DeviceHelper;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class StrickSSLHttpClientTest {

    private final String USER_AGENT = "agent";
    private final String FILE_PATH = "/filepath";
    private final String OAUTH_TOKEN = "OAuth some token";

    @Mock private DeviceHelper deviceHelper;
    @Mock private OAuth oAuth;
    @Mock private OkHttpClient okHttp;
    @Mock private Call httpCall;
    @Captor private ArgumentCaptor<Request> requestCaptor;

    private StrictSSLHttpClient httpClient;

    @Before
    public void setUp() throws Exception {
        httpClient = new StrictSSLHttpClient(okHttp, deviceHelper, oAuth);
        when(deviceHelper.getUserAgent()).thenReturn(USER_AGENT);
        when(oAuth.getAuthorizationHeaderValue()).thenReturn(OAUTH_TOKEN);
        when(okHttp.newCall(requestCaptor.capture())).thenReturn(httpCall);
    }

    @Test
    public void downloadFileRequestUsesGivenUrl() throws Exception {
        httpClient.getFileStream(FILE_PATH);

        expect(requestCaptor.getValue().urlString()).toMatch(FILE_PATH);
    }

    @Test
    public void downloadFileRequestIncludesDeviceHeader() throws Exception {
        httpClient.getFileStream(FILE_PATH);

        expect(requestCaptor.getValue().header("User-Agent")).toEqual(USER_AGENT);
    }

    @Test
    public void downloadFileRequestIncludesOAuthToken() throws Exception {
        httpClient.getFileStream(FILE_PATH);

        expect(requestCaptor.getValue().header("Authorization")).toEqual(OAUTH_TOKEN);
    }

}
