package com.soundcloud.android.analytics;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestHttpResponses;
import com.soundcloud.android.utils.DeviceHelper;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SimpleTrackingApiTest extends AndroidUnitTest {

    private TrackingRecord event;
    private SimpleTrackingApi simpleTrackingApi;

    private String fakeUrl;

    @Mock private DeviceHelper deviceHelper;
    @Mock private OkHttpClient httpClient;
    @Mock private Call httpCall;
    @Captor private ArgumentCaptor<Request> requestCaptor;

    @Before
    public void setup() throws IOException {
        simpleTrackingApi = new SimpleTrackingApi(httpClient, deviceHelper);
        fakeUrl = "http://fake_url";
        event = new TrackingRecord(1L, 1000L, "backend", fakeUrl);
        when(deviceHelper.getUserAgent()).thenReturn("SoundCloud-Android");
        when(httpClient.newCall(requestCaptor.capture())).thenReturn(httpCall);
    }

    @Test
    public void shouldTreatEntire2xxTo4xxStatusRangeAsSuccessSoWeDoNotRetryClientErrors() throws Exception {
        when(httpCall.execute()).thenReturn(
                TestHttpResponses.response(200).build(),
                TestHttpResponses.response(499).build(),
                TestHttpResponses.response(500).build()
        );
        TrackingRecord failedEvent = new TrackingRecord(2L, 1000L, "backend", fakeUrl);

        List<TrackingRecord> successes = simpleTrackingApi.pushToRemote(Arrays.asList(event, event, failedEvent));
        assertThat(successes).hasSize(2);
        assertThat(successes.get(0).getId()).isEqualTo(1L);
        assertThat(successes.get(1).getId()).isEqualTo(1L);
    }

    @Test
    public void shouldSetUserAgentHeader() throws Exception {
        when(httpCall.execute()).thenReturn(TestHttpResponses.response(200).build());

        simpleTrackingApi.pushToRemote(singletonList(event));

        assertThat(requestCaptor.getValue().headers("User-Agent").get(0)).isEqualTo("SoundCloud-Android");
    }

    @Test
    public void shouldBuildGETRequestForPromoted() throws Exception {
        when(httpCall.execute()).thenReturn(TestHttpResponses.response(200).build());
        TrackingRecord event = new TrackingRecord(1L, PromotedAnalyticsProvider.BACKEND_NAME, fakeUrl);

        simpleTrackingApi.pushToRemote(singletonList(event));

        assertThat(requestCaptor.getValue().method()).isEqualTo("GET");
    }

}
