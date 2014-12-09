package com.soundcloud.android.analytics;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.playcounts.PlayCountAnalyticsProvider;
import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
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
import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class TrackingApiTest {

    private TrackingRecord event;
    private TrackingApi trackingApi;

    private OkHttpClient httpClient = new OkHttpClient();
    private MockWebServer mockWebServer = new MockWebServer();
    private String fakeUrl;

    @Mock private DeviceHelper deviceHelper;

    @Before
    public void setup() throws IOException {
        trackingApi = new TrackingApi(httpClient, deviceHelper);
        mockWebServer.play();
        fakeUrl = mockWebServer.getUrl("").toString();
        event = new TrackingRecord(1L, 1000L, "backend", fakeUrl);
        when(deviceHelper.getUserAgent()).thenReturn("SoundCloud-Android");
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void shouldTreatEntire2xxTo4xxStatusRangeAsSuccessSoWeDoNotRetryClientErrors() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        mockWebServer.enqueue(new MockResponse().setResponseCode(499));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        TrackingRecord failedEvent = new TrackingRecord(2L, 1000L, "backend", fakeUrl);

        List<TrackingRecord> successes = trackingApi.pushToRemote(Arrays.asList(event, event, failedEvent));
        expect(successes).toNumber(2);
        expect(successes.get(0).getId()).toEqual(1L);
        expect(successes.get(1).getId()).toEqual(1L);
    }

    @Test
    public void shouldSetUserAgentHeader() throws Exception {
        mockWebServer.enqueue(new MockResponse());

        trackingApi.pushToRemote(Arrays.asList(event));
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getHeaders("User-Agent").get(0)).toEqual("SoundCloud-Android");
    }

    @Test
    public void shouldBuildHEADRequestForEventLogger() throws Exception {
        mockWebServer.enqueue(new MockResponse());
        TrackingRecord event = new TrackingRecord(1L, EventLoggerAnalyticsProvider.BACKEND_NAME, fakeUrl);

        trackingApi.pushToRemote(Arrays.asList(event));

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getMethod()).toEqual("HEAD");
    }

    @Test
    public void shouldBuildGETRequestForPromoted() throws Exception {
        mockWebServer.enqueue(new MockResponse());
        TrackingRecord event = new TrackingRecord(1L, PromotedAnalyticsProvider.BACKEND_NAME, fakeUrl);

        trackingApi.pushToRemote(Arrays.asList(event));

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getMethod()).toEqual("GET");
    }

    @Test
    public void shouldBuildPOSTRequestForPublicApiPlayCounts() throws Exception {
        mockWebServer.enqueue(new MockResponse());
        TrackingRecord event = new TrackingRecord(1L, PlayCountAnalyticsProvider.BACKEND_NAME, fakeUrl);

        trackingApi.pushToRemote(Arrays.asList(event));

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getMethod()).toEqual("POST");
        expect(recordedRequest.getBody().length).toEqual(0);
        expect(recordedRequest.getHeader("Content-Length")).toEqual("0");
    }
}
