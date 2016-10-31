package com.soundcloud.android.analytics;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
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

public class BatchTrackingApiTest extends AndroidUnitTest {

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
    public void setUp() {
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
        assertThat(successes).hasSize(BATCH_SIZE * 2);
    }

    @Test
    public void shouldSetUserAgentHeader() throws Exception {
        when(httpCall.execute()).thenReturn(TestHttpResponses.response(200).build());

        api.pushToRemote(singletonList(EVENT));

        assertThat(requestCaptor.getValue().headers("User-Agent").get(0)).isEqualTo("SoundCloud-Android");
    }

    @Test
    public void shouldBuildPostRequest() throws Exception {
        when(httpCall.execute()).thenReturn(TestHttpResponses.response(200).build());
        TrackingRecord event = new TrackingRecord(1L, EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME, DATA);

        api.pushToRemote(singletonList(event));

        assertThat(requestCaptor.getValue().method()).isEqualTo("POST");
    }

    @Test
    public void shouldBuildPOSTRequestForOneBatch() throws Exception {
        String jsonResult = "[{\"some\":\"data\"},{\"some\":\"data\"}]";

        when(httpCall.execute()).thenReturn(TestHttpResponses.response(200).build());

        TrackingRecord event1 = new TrackingRecord(1L, EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME, DATA);
        TrackingRecord event2 = new TrackingRecord(1L, EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME, DATA);

        api.pushToRemote(Arrays.asList(event1, event2));

        checkPostRequest(0, jsonResult);
    }

    @Test
    public void shouldBuildPOSTRequestForTwoBatch() throws Exception {
        String jsonResult1 = "[{\"some\":\"data\"},{\"some\":\"data\"}]";
        String jsonResult2 = "[{\"some\":\"data\"}]";

        when(httpCall.execute()).thenReturn(TestHttpResponses.response(200).build());

        TrackingRecord event1 = new TrackingRecord(1L, EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME, DATA);
        TrackingRecord event2 = new TrackingRecord(1L, EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME, DATA);
        TrackingRecord event3 = new TrackingRecord(1L, EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME, DATA);

        api.pushToRemote(Arrays.asList(event1, event2, event3));

        checkPostRequest(0, jsonResult1);
        checkPostRequest(1, jsonResult2);
    }

    private void checkPostRequest(int call, String expectedBody) throws IOException {
        assertThat(requestCaptor.getAllValues().get(call).method()).isEqualTo("POST");
        assertThat(requestCaptor.getAllValues()
                                .get(call)
                                .body()
                                .contentLength()).isEqualTo((long) expectedBody.length());
        assertThat(requestCaptor.getAllValues()
                                .get(call)
                                .body()
                                .contentType()
                                .toString()).isEqualTo("application/json");
    }
}
