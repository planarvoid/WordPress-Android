package com.soundcloud.android.analytics;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.playcounts.PlayCountAnalyticsProvider;
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
public class BatchTrackingApiTest {

    private static final String BATCH_URL = "http://batch-url";
    private static final String DATA = "{\"some\" : \"data\"}";
    private static final TrackingRecord EVENT = new TrackingRecord(1L, 1000L, "backend", DATA);
    private static final int BATCH_SIZE = 2;

    private BatchTrackingApi api;

    @Mock private OkHttpClient httpClient;
    @Mock private DeviceHelper deviceHelper;
    @Mock private Call httpCall;

    @Captor private ArgumentCaptor<Request> requestCaptor;

    @Before
    public void setUp() throws Exception {
        api = new BatchTrackingApi(httpClient, deviceHelper, BATCH_URL, BATCH_SIZE);
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

        List<TrackingRecord> successes = api.pushToRemote(Arrays.asList(EVENT, EVENT, EVENT, EVENT, EVENT, EVENT));
        expect(successes).toNumber(BATCH_SIZE * 2);
    }

    @Test
    public void shouldSetUserAgentHeader() throws Exception {
        when(httpCall.execute()).thenReturn(TestHttpResponses.response(200).build());

        api.pushToRemote(Arrays.asList(EVENT));

        expect(requestCaptor.getValue().headers("User-Agent").get(0)).toEqual("SoundCloud-Android");
    }

    @Test
    public void shouldBuildPostRequest() throws Exception {
        when(httpCall.execute()).thenReturn(TestHttpResponses.response(200).build());
        TrackingRecord event = new TrackingRecord(1L, EventLoggerAnalyticsProvider.BACKEND_NAME, DATA);

        api.pushToRemote(Arrays.asList(event));

        expect(requestCaptor.getValue().method()).toEqual("POST");
    }

    @Test
    public void shouldBuildPOSTRequestForOneBatch() throws Exception {
        String jsonResult = "[{\"some\":\"data\"},{\"some\":\"data\"}]";

        when(httpCall.execute()).thenReturn(TestHttpResponses.response(200).build());

        TrackingRecord event1 = new TrackingRecord(1L, PlayCountAnalyticsProvider.BACKEND_NAME, DATA);
        TrackingRecord event2 = new TrackingRecord(1L, PlayCountAnalyticsProvider.BACKEND_NAME, DATA);

        api.pushToRemote(Arrays.asList(event1, event2));

        checkPostRequest(0, jsonResult);
    }

    @Test
    public void shouldBuildPOSTRequestForTwoBatch() throws Exception {
        String jsonResult1 = "[{\"some\":\"data\"},{\"some\":\"data\"}]";
        String jsonResult2 = "[{\"some\":\"data\"}]";

        when(httpCall.execute()).thenReturn(TestHttpResponses.response(200).build());

        TrackingRecord event1 = new TrackingRecord(1L, PlayCountAnalyticsProvider.BACKEND_NAME, DATA);
        TrackingRecord event2 = new TrackingRecord(1L, PlayCountAnalyticsProvider.BACKEND_NAME, DATA);
        TrackingRecord event3 = new TrackingRecord(1L, PlayCountAnalyticsProvider.BACKEND_NAME, DATA);

        api.pushToRemote(Arrays.asList(event1, event2, event3));

        checkPostRequest(0, jsonResult1);
        checkPostRequest(1, jsonResult2);
    }

    private void checkPostRequest(int call, String expectedBody) throws IOException {
        expect(requestCaptor.getAllValues().get(call).method()).toEqual("POST");
        expect(requestCaptor.getAllValues().get(call).body().contentLength()).toEqual((long) expectedBody.length());
        expect(requestCaptor.getAllValues().get(call).body().contentType().toString()).toEqual("application/json");
    }
}