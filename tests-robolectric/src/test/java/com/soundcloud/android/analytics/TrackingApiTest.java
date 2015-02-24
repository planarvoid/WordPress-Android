package com.soundcloud.android.analytics;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.playcounts.PlayCountAnalyticsProvider;
import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.TestHttpResponses;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class TrackingApiTest {

    private TrackingRecord event;
    private TrackingApi trackingApi;

    private String fakeUrl;

    @Mock private DeviceHelper deviceHelper;
    @Mock private OkHttpClient httpClient;
    @Mock private Call httpCall;
    @Captor private ArgumentCaptor<Request> requestCaptor;

    @Before
    public void setup() throws IOException {
        trackingApi = new TrackingApi(httpClient, deviceHelper);
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

        List<TrackingRecord> successes = trackingApi.pushToRemote(Arrays.asList(event, event, failedEvent));
        expect(successes).toNumber(2);
        expect(successes.get(0).getId()).toEqual(1L);
        expect(successes.get(1).getId()).toEqual(1L);
    }

    @Test
    public void shouldSetUserAgentHeader() throws Exception {
        when(httpCall.execute()).thenReturn(TestHttpResponses.response(200).build());

        trackingApi.pushToRemote(Arrays.asList(event));

        expect(requestCaptor.getValue().headers("User-Agent").get(0)).toEqual("SoundCloud-Android");
    }

    @Test
    public void shouldBuildHEADRequestForEventLogger() throws Exception {
        when(httpCall.execute()).thenReturn(TestHttpResponses.response(200).build());
        TrackingRecord event = new TrackingRecord(1L, EventLoggerAnalyticsProvider.BACKEND_NAME, fakeUrl);

        trackingApi.pushToRemote(Arrays.asList(event));

        expect(requestCaptor.getValue().method()).toEqual("HEAD");
    }

    @Test
    public void shouldBuildGETRequestForPromoted() throws Exception {
        when(httpCall.execute()).thenReturn(TestHttpResponses.response(200).build());
        TrackingRecord event = new TrackingRecord(1L, PromotedAnalyticsProvider.BACKEND_NAME, fakeUrl);

        trackingApi.pushToRemote(Arrays.asList(event));

        expect(requestCaptor.getValue().method()).toEqual("GET");
    }

    @Test
    public void shouldBuildPOSTRequestForPublicApiPlayCounts() throws Exception {
        when(httpCall.execute()).thenReturn(TestHttpResponses.response(200).build());
        TrackingRecord event = new TrackingRecord(1L, PlayCountAnalyticsProvider.BACKEND_NAME, fakeUrl);

        trackingApi.pushToRemote(Arrays.asList(event));

        expect(requestCaptor.getValue().method()).toEqual("POST");
        expect(requestCaptor.getValue().body().contentLength()).toEqual(0L);
        expect(requestCaptor.getValue().header("Content-Length")).toEqual("0");
    }
}
