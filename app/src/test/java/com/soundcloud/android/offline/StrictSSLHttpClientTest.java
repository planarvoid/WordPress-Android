package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DeviceHelper;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class StrictSSLHttpClientTest extends AndroidUnitTest {

    private final String USER_AGENT = "agent";
    private final String FILE_PATH = "/filepath";
    private final String OAUTH_TOKEN = "OAuth some token";

    private StrictSSLHttpClient httpClient;

    @Mock private DeviceHelper deviceHelper;
    @Mock private OAuth oAuth;
    @Mock private OkHttpClient okHttp;
    @Mock private Call httpCall;

    @Captor private ArgumentCaptor<Request> requestCaptor;

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

        assertThat(requestCaptor.getValue().urlString()).isEqualTo(FILE_PATH);
    }

    @Test
    public void downloadFileRequestIncludesDeviceHeader() throws Exception {
        httpClient.getFileStream(FILE_PATH);

        assertThat(requestCaptor.getValue().headers("User-Agent")).containsExactly(USER_AGENT);
    }

    @Test
    public void downloadFileRequestIncludesOAuthToken() throws Exception {
        httpClient.getFileStream(FILE_PATH);

        assertThat(requestCaptor.getValue().headers("Authorization")).containsExactly(OAUTH_TOKEN);
    }

}
